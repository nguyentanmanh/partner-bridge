package com.manh.partnerbridge.payment.application.exception;

import com.manh.partnerbridge.payment.domain.exception.ErrorCode;

public class ExternalSystemException extends ApplicationException {
    public ExternalSystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
