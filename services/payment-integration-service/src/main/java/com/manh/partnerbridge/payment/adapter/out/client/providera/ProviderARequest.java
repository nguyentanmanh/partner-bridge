package com.manh.partnerbridge.payment.adapter.out.client.providera;

import java.math.BigDecimal;

public record ProviderARequest(String merchantRef, BigDecimal amount, String currency, String note) {}
