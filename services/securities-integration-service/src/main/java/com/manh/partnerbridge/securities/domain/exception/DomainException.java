package com.manh.partnerbridge.securities.domain.exception;

public abstract class DomainException extends RuntimeException {
    private final ErrorCode errorCode;
    protected DomainException(ErrorCode errorCode) { super(errorCode.code()); this.errorCode = errorCode; }
    public ErrorCode errorCode() { return errorCode; }
}
