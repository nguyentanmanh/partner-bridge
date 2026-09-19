package com.manh.partnerbridge.payment.application.port.out;

import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.domain.model.PaymentStatus;

public interface PaymentProviderPort {
    String providerCode();
    ProviderResult create(CreatePaymentCommand command, String requestId);
    record ProviderResult(String transactionId, PaymentStatus status) {}
}
