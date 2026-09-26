package com.manh.partnerbridge.banking.application.port.in;

import com.manh.partnerbridge.banking.application.command.CreateTransferCommand;
import com.manh.partnerbridge.banking.domain.model.BankAccount;
import com.manh.partnerbridge.banking.domain.model.BankTransfer;

public interface BankingUseCase {
    BankAccount getAccount(String accountId, String requestId);
    BankTransfer createTransfer(CreateTransferCommand command, String requestId);
}
