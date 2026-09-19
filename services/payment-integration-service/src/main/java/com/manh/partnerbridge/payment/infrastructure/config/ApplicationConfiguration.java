package com.manh.partnerbridge.payment.infrastructure.config;

import com.manh.partnerbridge.payment.adapter.out.persistence.InMemoryItemAdapter;
import com.manh.partnerbridge.payment.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.payment.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.payment.application.usecase.GetItemService;
import com.manh.partnerbridge.payment.adapter.out.client.ProviderAAdapter;
import com.manh.partnerbridge.payment.adapter.out.client.ProviderBAdapter;
import com.manh.partnerbridge.payment.application.port.in.PaymentUseCase;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;
import com.manh.partnerbridge.payment.application.port.out.PaymentMessagesPort;
import com.manh.partnerbridge.payment.infrastructure.i18n.MessageResolver;
import com.manh.partnerbridge.payment.application.usecase.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean
    PaymentProviderPort providerA(ObjectMapper mapper,
        @Value("${partnerbridge.provider-a.base-url}") String url,
        @Value("${partnerbridge.provider-a.api-key}") String key,
        @Value("${partnerbridge.provider-a.connect-timeout}") Duration connectTimeout,
        @Value("${partnerbridge.provider-a.response-timeout}") Duration responseTimeout) {
        return new ProviderAAdapter(mapper, url, key, connectTimeout, responseTimeout);
    }

    @Bean
    PaymentProviderPort providerB(ObjectMapper mapper,
        @Value("${partnerbridge.provider-b.base-url}") String url,
        @Value("${partnerbridge.provider-b.client-id}") String clientId,
        @Value("${partnerbridge.provider-b.signature}") String signature,
        @Value("${partnerbridge.provider-b.connect-timeout}") Duration connectTimeout,
        @Value("${partnerbridge.provider-b.response-timeout}") Duration responseTimeout) {
        return new ProviderBAdapter(mapper, url, clientId, signature, connectTimeout, responseTimeout);
    }

    @Bean
    Clock paymentClock() {
        return Clock.systemUTC();
    }

    @Bean
    PaymentMessagesPort paymentMessagesPort(MessageResolver resolver) {
        return (code, locale) -> resolver.resolve(code.messageKey(), locale);
    }

    @Bean
    PaymentUseCase paymentUseCase(List<PaymentProviderPort> providers, Clock paymentClock, PaymentMessagesPort messages,
        @Value("${partnerbridge.idempotency.retention}") Duration retention) {
        return new PaymentService(providers, paymentClock, retention, messages);
    }
    @Bean
    ItemQueryPort itemQueryPort() {
        return new InMemoryItemAdapter();
    }

    @Bean
    GetItemUseCase getItemUseCase(ItemQueryPort port) {
        return new GetItemService(port);
    }
}
