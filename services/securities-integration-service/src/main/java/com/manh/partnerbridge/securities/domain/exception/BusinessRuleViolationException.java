package com.manh.partnerbridge.securities.domain.exception;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(ErrorCode errorCode) { super(errorCode); }
}
