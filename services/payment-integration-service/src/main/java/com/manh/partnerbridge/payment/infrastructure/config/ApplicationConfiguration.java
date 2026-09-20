package com.manh.partnerbridge.payment.infrastructure.config;

import com.manh.partnerbridge.payment.adapter.out.persistence.InMemoryItemAdapter;
import com.manh.partnerbridge.payment.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.payment.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.payment.application.usecase.GetItemService;
import com.manh.partnerbridge.payment.application.port.in.PaymentUseCase;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;
import com.manh.partnerbridge.payment.application.port.out.PaymentMessagesPort;
import com.manh.partnerbridge.payment.infrastructure.i18n.MessageResolver;
import com.manh.partnerbridge.payment.application.usecase.PaymentService;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
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
