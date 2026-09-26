package com.manh.partnerbridge.education.application.exception;

import com.manh.partnerbridge.education.domain.exception.CommonErrorCode;

public final class ExternalSystemTimeoutException extends ExternalSystemException {
    public ExternalSystemTimeoutException(Throwable cause) { super(CommonErrorCode.EXTERNAL_TIMEOUT, cause); }
}
