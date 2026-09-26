package com.manh.partnerbridge.securities.application.exception;

import com.manh.partnerbridge.securities.domain.exception.CommonErrorCode;

public final class TemporaryIntegrationException extends ExternalSystemException {
    public TemporaryIntegrationException(Throwable cause) { super(CommonErrorCode.EXTERNAL_UNAVAILABLE, cause); }
}
