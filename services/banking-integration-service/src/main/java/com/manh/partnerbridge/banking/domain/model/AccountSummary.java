package com.manh.partnerbridge.banking.domain.model;

import java.util.List;

public record AccountSummary(BankAccount account, List<RecentTransaction> recentTransactions) {
    public AccountSummary {
        recentTransactions = List.copyOf(recentTransactions);
    }
}
