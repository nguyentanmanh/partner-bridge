package com.manh.partnerbridge.payment.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.payment.adapter.in.rest.CreatePaymentRequest;
import com.manh.partnerbridge.payment.adapter.in.rest.PaymentController;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.port.in.PaymentUseCase;
import com.manh.partnerbridge.payment.infrastructure.i18n.MessageResolver;
import com.manh.partnerbridge.payment.infrastructure.observability.TraceIdProvider;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.support.StaticMessageSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentConfigurationTest {
    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
            .withInitializer(context -> context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance()))
            .withUserConfiguration(ApplicationConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(MessageResolver.class, () -> new MessageResolver(new StaticMessageSource()));
    }

    @Test void defaultRetentionAndInjectableClockAreBound() {
        runner().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PaymentUseCase.class).hasSingleBean(Clock.class);
            assertThat(context.getEnvironment().getProperty("partnerbridge.idempotency.retention")).isEqualTo("24h");
        });
    }

    @Test void positiveRetentionOverrideStartsButZeroAndNegativeFailStartup() {
        runner().withPropertyValues("partnerbridge.idempotency.retention=17s")
            .run(context -> assertThat(context).hasNotFailed());
        for (String retention : new String[] {"0s", "-1s", "not-a-duration"}) {
            runner().withPropertyValues("partnerbridge.idempotency.retention=" + retention)
                .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test void configuredClientIdIsPassedToUseCase() {
        PaymentUseCase useCase = mock(PaymentUseCase.class);
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PaymentController.class)
            .withBean(PaymentUseCase.class, () -> useCase)
            .withBean(TraceIdProvider.class, () -> mock(TraceIdProvider.class))
            .withPropertyValues("partnerbridge.client.default-id=configured-local-client")
            .run(context -> {
                assertThat(context).hasNotFailed();
                context.getBean(PaymentController.class).create("request", "key", "untrusted-header", new CreatePaymentRequest(
                    "PROVIDER_A", "ORDER-1", new CreatePaymentRequest.Amount("100000", "VND"), null));
                verify(useCase).create(any(), eq("configured-local-client"), eq("key"), any());
            });
    }

    @Test void trustedIdentityModeRejectsMissingHeaderAndUsesAuthenticatedScope() {
        PaymentUseCase useCase = mock(PaymentUseCase.class);
        var request = new CreatePaymentRequest("PROVIDER_A", "ORDER-1",
            new CreatePaymentRequest.Amount("100000", "VND"), null);
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PaymentController.class)
            .withBean(PaymentUseCase.class, () -> useCase)
            .withBean(TraceIdProvider.class, () -> mock(TraceIdProvider.class))
            .withPropertyValues("partnerbridge.client.default-id=local-poc-client",
                "partnerbridge.client.trusted-identity-required=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                var controller = context.getBean(PaymentController.class);
                assertThrows(PaymentException.class, () -> controller.create("request", "key", null, request));
                assertThrows(PaymentException.class, () -> controller.get("request", null,
                    "00000000-0000-0000-0000-000000000000"));
                verifyNoInteractions(useCase);
                controller.create("request", "key", "consumer-a", request);
                verify(useCase).create(any(), eq("consumer-a"), eq("key"), any());
                controller.get("request", "consumer-a", "00000000-0000-0000-0000-000000000000");
                verify(useCase).get("00000000-0000-0000-0000-000000000000", "consumer-a");
            });
    }
}
