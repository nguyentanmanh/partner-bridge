package com.manh.partnerbridge.education.application.exception;

import com.manh.partnerbridge.education.domain.exception.CommonErrorCode;

public final class TemporaryIntegrationException extends ExternalSystemException {
    public TemporaryIntegrationException(Throwable cause) { super(CommonErrorCode.EXTERNAL_UNAVAILABLE, cause); }
}
