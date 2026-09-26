package com.manh.partnerbridge.securities.application.exception;

import com.manh.partnerbridge.securities.domain.exception.CommonErrorCode;

public final class ExternalSystemTimeoutException extends ExternalSystemException {
    public ExternalSystemTimeoutException(Throwable cause) { super(CommonErrorCode.EXTERNAL_TIMEOUT, cause); }
}
