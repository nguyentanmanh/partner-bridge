package com.manh.partnerbridge.banking.domain.exception;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(ErrorCode errorCode) { super(errorCode); }
}
