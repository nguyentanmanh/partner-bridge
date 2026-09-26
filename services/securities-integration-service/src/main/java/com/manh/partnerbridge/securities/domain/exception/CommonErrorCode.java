package com.manh.partnerbridge.securities.domain.exception;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("SEC-COMMON-001", "error.common.invalid-request"),
    INTERNAL_ERROR("SEC-COMMON-002", "error.common.internal"),
    UNAUTHORIZED("SEC-AUTH-001", "error.auth.unauthorized"),
    ACCESS_DENIED("SEC-AUTH-002", "error.auth.access-denied"),
    EXTERNAL_TIMEOUT("SEC-INTEGRATION-001", "error.integration.timeout"),
    EXTERNAL_UNAVAILABLE("SEC-INTEGRATION-002", "error.integration.unavailable");
    private final String code; private final String messageKey;
    CommonErrorCode(String code, String messageKey) { this.code = code; this.messageKey = messageKey; }
    public String code() { return code; } public String messageKey() { return messageKey; }
}
