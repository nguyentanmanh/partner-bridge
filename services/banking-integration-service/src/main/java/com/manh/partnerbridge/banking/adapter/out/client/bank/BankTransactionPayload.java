package com.manh.partnerbridge.banking.adapter.out.client.bank;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BankTransactionPayload(String transactionId, BigDecimal amount, String currency,
                                     OffsetDateTime transactionDate) {}
