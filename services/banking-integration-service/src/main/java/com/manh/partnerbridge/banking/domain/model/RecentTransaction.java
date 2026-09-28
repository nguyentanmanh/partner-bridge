package com.manh.partnerbridge.banking.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecentTransaction(String transactionId, BigDecimal amount, String currency,
                                OffsetDateTime transactionDate) {}
