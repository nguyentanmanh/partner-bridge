package com.manh.partnerbridge.payment.application.exception;

import com.manh.partnerbridge.payment.domain.exception.CommonErrorCode;

public final class TemporaryIntegrationException extends ExternalSystemException {
    public TemporaryIntegrationException(Throwable cause) {
        super(CommonErrorCode.EXTERNAL_UNAVAILABLE, cause);
    }
}
