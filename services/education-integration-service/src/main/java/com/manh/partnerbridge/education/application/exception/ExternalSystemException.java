package com.manh.partnerbridge.education.application.exception;

import com.manh.partnerbridge.education.domain.exception.ErrorCode;

public class ExternalSystemException extends ApplicationException {
    public ExternalSystemException(ErrorCode errorCode, Throwable cause) { super(errorCode, cause); }
}
