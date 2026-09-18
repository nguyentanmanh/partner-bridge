package com.manh.partnerbridge.payment.application.exception;

import com.manh.partnerbridge.payment.domain.exception.CommonErrorCode;

public final class ExternalSystemTimeoutException extends ExternalSystemException {
    public ExternalSystemTimeoutException(Throwable cause) {
        super(CommonErrorCode.EXTERNAL_TIMEOUT, cause);
    }
}
