package com.manh.partnerbridge.banking.application.usecase;

import com.manh.partnerbridge.banking.application.command.CreateTransferCommand;
import com.manh.partnerbridge.banking.application.port.in.BankingUseCase;
import com.manh.partnerbridge.banking.application.port.out.BankProviderPort;
import com.manh.partnerbridge.banking.domain.model.BankAccount;
import com.manh.partnerbridge.banking.domain.model.BankTransfer;
import com.manh.partnerbridge.banking.domain.model.AccountSummary;
import com.manh.partnerbridge.banking.domain.model.RecentTransaction;
import java.util.List;

public final class BankingService implements BankingUseCase {
    private final BankProviderPort provider;
    public BankingService(BankProviderPort provider) { this.provider = provider; }
    @Override public BankAccount getAccount(String accountId, String requestId) {
        return provider.getAccount(accountId, requestId);
    }
    @Override public List<RecentTransaction> getTransactions(String accountId, String requestId) {
        return provider.getTransactions(accountId, requestId);
    }
    @Override public AccountSummary getAccountSummary(String accountId, String requestId) {
        BankAccount account = provider.getAccount(accountId, requestId);
        return new AccountSummary(account, provider.getTransactions(accountId, requestId));
    }
    @Override public BankTransfer createTransfer(CreateTransferCommand command, String requestId) {
        return provider.createTransfer(command, requestId);
    }
}
