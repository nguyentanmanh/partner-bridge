package com.manh.partnerbridge.banking.application.exception;

import com.manh.partnerbridge.banking.domain.exception.CommonErrorCode;

public final class ExternalSystemTimeoutException extends ExternalSystemException {
    public ExternalSystemTimeoutException(Throwable cause) { super(CommonErrorCode.EXTERNAL_TIMEOUT, cause); }
}
