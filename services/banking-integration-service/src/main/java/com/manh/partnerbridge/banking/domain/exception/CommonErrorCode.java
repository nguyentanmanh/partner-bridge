package com.manh.partnerbridge.banking.domain.exception;

public enum CommonErrorCode implements ErrorCode {
    INVALID_REQUEST("BNK-COMMON-001", "error.common.invalid-request"),
    INTERNAL_ERROR("BNK-COMMON-002", "error.common.internal"),
    UNAUTHORIZED("BNK-AUTH-001", "error.auth.unauthorized"),
    ACCESS_DENIED("BNK-AUTH-002", "error.auth.access-denied"),
    EXTERNAL_TIMEOUT("BNK-INTEGRATION-001", "error.integration.timeout"),
    EXTERNAL_UNAVAILABLE("BNK-INTEGRATION-002", "error.integration.unavailable"),
    EXTERNAL_INVALID_RESPONSE("BNK-INTEGRATION-003", "error.integration.invalid-response");
    private final String code; private final String messageKey;
    CommonErrorCode(String code, String messageKey) { this.code = code; this.messageKey = messageKey; }
    public String code() { return code; } public String messageKey() { return messageKey; }
}
