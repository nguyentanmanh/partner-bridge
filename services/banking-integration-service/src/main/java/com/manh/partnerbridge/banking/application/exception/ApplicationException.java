package com.manh.partnerbridge.banking.application.exception;

import com.manh.partnerbridge.banking.domain.exception.ErrorCode;

public class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;
    public ApplicationException(ErrorCode errorCode, Throwable cause) { super(errorCode.code(), cause); this.errorCode = errorCode; }
    public ErrorCode errorCode() { return errorCode; }
}
