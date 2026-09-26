package com.manh.partnerbridge.education.domain.exception;

public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(ErrorCode errorCode) { super(errorCode); }
}
