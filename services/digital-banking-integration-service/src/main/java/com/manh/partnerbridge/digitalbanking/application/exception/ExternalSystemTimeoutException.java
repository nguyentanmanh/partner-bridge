package com.manh.partnerbridge.digitalbanking.application.exception;

import com.manh.partnerbridge.digitalbanking.domain.exception.CommonErrorCode;

public final class ExternalSystemTimeoutException extends ExternalSystemException {
    public ExternalSystemTimeoutException(Throwable cause) { super(CommonErrorCode.EXTERNAL_TIMEOUT, cause); }
}
