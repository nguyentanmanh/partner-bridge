package com.manh.partnerbridge.banking.adapter.in.rest;

import com.manh.partnerbridge.banking.domain.model.BankAccount;
import java.math.BigDecimal;

public record BankAccountResponse(String accountId, String holderName, BigDecimal balance,
                                  String currency, String status) {
    static BankAccountResponse from(BankAccount account) {
        return new BankAccountResponse(account.accountId(), account.holderName(), account.balance(),
                account.currency(), account.status());
    }
}
