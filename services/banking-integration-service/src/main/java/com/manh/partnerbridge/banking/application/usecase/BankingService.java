package com.manh.partnerbridge.banking.application.usecase;

import com.manh.partnerbridge.banking.application.command.CreateTransferCommand;
import com.manh.partnerbridge.banking.application.port.in.BankingUseCase;
import com.manh.partnerbridge.banking.application.port.out.BankProviderPort;
import com.manh.partnerbridge.banking.domain.model.BankAccount;
import com.manh.partnerbridge.banking.domain.model.BankTransfer;

public final class BankingService implements BankingUseCase {
    private final BankProviderPort provider;
    public BankingService(BankProviderPort provider) { this.provider = provider; }
    @Override public BankAccount getAccount(String accountId, String requestId) {
        return provider.getAccount(accountId, requestId);
    }
    @Override public BankTransfer createTransfer(CreateTransferCommand command, String requestId) {
        return provider.createTransfer(command, requestId);
    }
}
