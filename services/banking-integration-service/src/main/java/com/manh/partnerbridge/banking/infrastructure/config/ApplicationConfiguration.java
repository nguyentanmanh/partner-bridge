package com.manh.partnerbridge.banking.infrastructure.config;

import com.manh.partnerbridge.banking.adapter.out.persistence.InMemoryItemAdapter;
import com.manh.partnerbridge.banking.application.port.in.GetItemUseCase;
import com.manh.partnerbridge.banking.application.port.out.ItemQueryPort;
import com.manh.partnerbridge.banking.application.usecase.GetItemService;
import com.manh.partnerbridge.banking.application.port.in.BankingUseCase;
import com.manh.partnerbridge.banking.application.port.out.BankProviderPort;
import com.manh.partnerbridge.banking.application.usecase.BankingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean ItemQueryPort itemQueryPort() { return new InMemoryItemAdapter(); }
    @Bean GetItemUseCase getItemUseCase(ItemQueryPort port) { return new GetItemService(port); }
    @Bean BankingUseCase bankingUseCase(BankProviderPort port) { return new BankingService(port); }
}
