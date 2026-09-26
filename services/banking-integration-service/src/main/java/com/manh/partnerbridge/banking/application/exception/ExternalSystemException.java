package com.manh.partnerbridge.banking.application.exception;

import com.manh.partnerbridge.banking.domain.exception.ErrorCode;

public class ExternalSystemException extends ApplicationException {
    public ExternalSystemException(ErrorCode errorCode, Throwable cause) { super(errorCode, cause); }
}
