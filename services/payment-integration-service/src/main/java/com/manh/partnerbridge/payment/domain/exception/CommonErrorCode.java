package com.manh.partnerbridge.payment.domain.exception;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("PAY-COMMON-001", "error.common.invalid-request"),
    INTERNAL_ERROR("PAY-COMMON-002", "error.common.internal"),
    UNAUTHORIZED("PAY-AUTH-001", "error.auth.unauthorized"),
    ACCESS_DENIED("PAY-AUTH-002", "error.auth.access-denied"),
    EXTERNAL_TIMEOUT("PAY-INTEGRATION-001", "error.integration.timeout"),
    EXTERNAL_UNAVAILABLE("PAY-INTEGRATION-002", "error.integration.unavailable");
    private final String code;
    private final String messageKey;

    CommonErrorCode(String code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    public String code() {
        return code;
    }

    public String messageKey() {
        return messageKey;
    }
}
