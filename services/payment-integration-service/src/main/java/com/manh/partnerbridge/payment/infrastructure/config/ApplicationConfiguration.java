package com.manh.partnerbridge.payment.infrastructure.config;

import com.manh.partnerbridge.payment.adapter.out.persistence.InMemoryItemAdapter;
import com.manh.partnerbridge.payment.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.payment.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.payment.application.usecase.GetItemService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean
    ItemQueryPort itemQueryPort() {
        return new InMemoryItemAdapter();
    }

    @Bean
    GetItemUseCase getItemUseCase(ItemQueryPort port) {
        return new GetItemService(port);
    }
}
