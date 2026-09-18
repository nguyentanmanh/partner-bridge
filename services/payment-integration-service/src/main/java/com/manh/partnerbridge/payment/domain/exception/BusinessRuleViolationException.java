package com.manh.partnerbridge.payment.domain.exception;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
