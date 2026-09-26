package com.manh.partnerbridge.digitalbanking.domain.exception;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(ErrorCode errorCode) { super(errorCode); }
}
