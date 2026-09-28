package com.manh.partnerbridge.banking.application.port.out;

import com.manh.partnerbridge.banking.application.command.CreateTransferCommand;
import com.manh.partnerbridge.banking.domain.model.BankAccount;
import com.manh.partnerbridge.banking.domain.model.BankTransfer;
import com.manh.partnerbridge.banking.domain.model.RecentTransaction;
import java.util.List;

public interface BankProviderPort {
    BankAccount getAccount(String accountId, String requestId);
    List<RecentTransaction> getTransactions(String accountId, String requestId);
    BankTransfer createTransfer(CreateTransferCommand command, String requestId);
}
