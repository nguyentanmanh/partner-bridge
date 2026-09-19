package com.manh.partnerbridge.payment.domain.model;

import java.time.Instant;

public record Payment(String paymentId,
                      String merchantReference,
                      String providerCode,
                      String providerTransactionId,
                      Money amount,
                      PaymentStatus status,
                      Instant createdAt,
                      Instant updatedAt) {
}
