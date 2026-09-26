package com.manh.partnerbridge.securities.application.exception;

import com.manh.partnerbridge.securities.domain.exception.ErrorCode;

public class ExternalSystemException extends ApplicationException {
    public ExternalSystemException(ErrorCode errorCode, Throwable cause) { super(errorCode, cause); }
}
