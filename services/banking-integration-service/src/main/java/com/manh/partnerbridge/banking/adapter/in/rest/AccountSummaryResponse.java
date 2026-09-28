package com.manh.partnerbridge.banking.adapter.in.rest;

import com.manh.partnerbridge.banking.domain.model.AccountSummary;
import java.math.BigDecimal;
import java.util.List;

public record AccountSummaryResponse(Account account, List<RecentTransactionResponse> recentTransactions) {
    public record Account(String accountNo, BigDecimal balance) {}

    static AccountSummaryResponse from(AccountSummary summary) {
        return new AccountSummaryResponse(
                new Account(summary.account().accountId(), summary.account().balance()),
                summary.recentTransactions().stream().map(RecentTransactionResponse::from).toList());
    }
}
