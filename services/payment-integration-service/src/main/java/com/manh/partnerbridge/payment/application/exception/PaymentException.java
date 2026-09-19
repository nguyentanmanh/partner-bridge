package com.manh.partnerbridge.payment.application.exception;

import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.application.query.PaymentFailure;

public final class PaymentException extends ApplicationException {
    private final PaymentFailure recordedFailure;

    public PaymentException(PaymentErrorCode code) {
        super(code, null);
        this.recordedFailure = null;
    }

    public PaymentException(PaymentFailure failure) {
        super(failure.code(), null);
        this.recordedFailure = failure;
    }

    public PaymentFailure recordedFailure() { return recordedFailure; }

    @Override
    public PaymentErrorCode errorCode() { return (PaymentErrorCode) super.errorCode(); }
}
