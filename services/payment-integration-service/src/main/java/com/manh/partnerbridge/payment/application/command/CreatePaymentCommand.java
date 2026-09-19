package com.manh.partnerbridge.payment.application.command;

import com.manh.partnerbridge.payment.domain.model.Money;

public record CreatePaymentCommand(String providerCode, String merchantReference,
                                   Money amount, String description) {}
