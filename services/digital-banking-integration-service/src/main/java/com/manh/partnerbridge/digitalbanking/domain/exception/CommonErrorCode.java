package com.manh.partnerbridge.digitalbanking.domain.exception;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("DBK-COMMON-001", "error.common.invalid-request"),
    INTERNAL_ERROR("DBK-COMMON-002", "error.common.internal"),
    UNAUTHORIZED("DBK-AUTH-001", "error.auth.unauthorized"),
    ACCESS_DENIED("DBK-AUTH-002", "error.auth.access-denied"),
    EXTERNAL_TIMEOUT("DBK-INTEGRATION-001", "error.integration.timeout"),
    EXTERNAL_UNAVAILABLE("DBK-INTEGRATION-002", "error.integration.unavailable");
    private final String code; private final String messageKey;
    CommonErrorCode(String code, String messageKey) { this.code = code; this.messageKey = messageKey; }
    public String code() { return code; } public String messageKey() { return messageKey; }
}
