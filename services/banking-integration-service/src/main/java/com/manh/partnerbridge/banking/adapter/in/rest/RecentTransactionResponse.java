package com.manh.partnerbridge.banking.adapter.in.rest;

import com.manh.partnerbridge.banking.domain.model.RecentTransaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecentTransactionResponse(String transactionId, BigDecimal amount, String currency,
                                        OffsetDateTime transactionDate) {
    static RecentTransactionResponse from(RecentTransaction transaction) {
        return new RecentTransactionResponse(transaction.transactionId(), transaction.amount(),
                transaction.currency(), transaction.transactionDate());
    }
}
