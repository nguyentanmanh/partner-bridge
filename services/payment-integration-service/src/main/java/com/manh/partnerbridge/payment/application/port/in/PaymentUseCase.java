package com.manh.partnerbridge.payment.application.port.in;

import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.command.PaymentRequestContext;
import com.manh.partnerbridge.payment.domain.model.Payment;

public interface PaymentUseCase {
    Payment create(CreatePaymentCommand command, String clientId, String idempotencyKey, PaymentRequestContext context);
    Payment get(String paymentId, String clientId);
}
