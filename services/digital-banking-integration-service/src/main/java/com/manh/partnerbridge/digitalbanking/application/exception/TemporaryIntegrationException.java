package com.manh.partnerbridge.digitalbanking.application.exception;

import com.manh.partnerbridge.digitalbanking.domain.exception.CommonErrorCode;

public final class TemporaryIntegrationException extends ExternalSystemException {
    public TemporaryIntegrationException(Throwable cause) { super(CommonErrorCode.EXTERNAL_UNAVAILABLE, cause); }
}
