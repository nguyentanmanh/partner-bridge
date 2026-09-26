package com.manh.partnerbridge.education.domain.exception;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("EDU-COMMON-001", "error.common.invalid-request"),
    INTERNAL_ERROR("EDU-COMMON-002", "error.common.internal"),
    UNAUTHORIZED("EDU-AUTH-001", "error.auth.unauthorized"),
    ACCESS_DENIED("EDU-AUTH-002", "error.auth.access-denied"),
    EXTERNAL_TIMEOUT("EDU-INTEGRATION-001", "error.integration.timeout"),
    EXTERNAL_UNAVAILABLE("EDU-INTEGRATION-002", "error.integration.unavailable");
    private final String code; private final String messageKey;
    CommonErrorCode(String code, String messageKey) { this.code = code; this.messageKey = messageKey; }
    public String code() { return code; } public String messageKey() { return messageKey; }
}
