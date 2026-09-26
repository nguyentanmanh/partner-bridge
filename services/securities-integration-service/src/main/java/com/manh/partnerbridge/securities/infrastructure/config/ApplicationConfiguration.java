package com.manh.partnerbridge.securities.infrastructure.config;

import com.manh.partnerbridge.securities.adapter.out.persistence.InMemoryItemAdapter;
import com.manh.partnerbridge.securities.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.securities.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.securities.application.usecase.GetItemService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean ItemQueryPort itemQueryPort() { return new InMemoryItemAdapter(); }
    @Bean GetItemUseCase getItemUseCase(ItemQueryPort port) { return new GetItemService(port); }
}
