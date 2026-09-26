package com.manh.partnerbridge.banking.application.exception;

import com.manh.partnerbridge.banking.domain.exception.CommonErrorCode;

public final class TemporaryIntegrationException extends ExternalSystemException {
    public TemporaryIntegrationException(Throwable cause) { super(CommonErrorCode.EXTERNAL_UNAVAILABLE, cause); }
}
