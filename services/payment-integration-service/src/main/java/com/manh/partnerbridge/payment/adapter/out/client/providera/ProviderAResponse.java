package com.manh.partnerbridge.payment.adapter.out.client.providera;

import java.math.BigDecimal;

public record ProviderAResponse(String code, String transactionId, String paymentStatus,
                                BigDecimal amount, String currency) {}
