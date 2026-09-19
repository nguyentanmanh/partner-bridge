package com.manh.partnerbridge.payment.domain.exception;

public enum PaymentErrorCode implements ErrorCode {
    INVALID_REQUEST("PAY-VALIDATION-001", "error.payment.invalid-request"),
    NOT_FOUND("PAY-PAYMENT-001", "error.payment.not-found"),
    DUPLICATE_REFERENCE("PAY-PAYMENT-002", "error.payment.duplicate-reference"),
    IDEMPOTENCY_CONFLICT("PAY-IDEMPOTENCY-001", "error.payment.idempotency-conflict"),
    UNSUPPORTED_PROVIDER("PAY-BUSINESS-002", "error.payment.unsupported-provider"),
    PROVIDER_REJECTED("PAY-BUSINESS-003", "error.payment.provider-rejected"),
    PROVIDER_AUTH("PAY-PROVIDER-001", "error.payment.provider-auth"),
    INVALID_PROVIDER_RESPONSE("PAY-PROVIDER-002", "error.payment.invalid-provider-response"),
    PROVIDER_UNAVAILABLE("PAY-PROVIDER-003", "error.payment.provider-unavailable"),
    PROVIDER_TIMEOUT("PAY-PROVIDER-004", "error.payment.provider-timeout"),
    INTERNAL_ERROR("PAY-INTERNAL-001", "error.common.internal");

    private final String code;
    private final String messageKey;
    PaymentErrorCode(String code, String messageKey) { this.code = code; this.messageKey = messageKey; }
    public String code() { return code; }
    public String messageKey() { return messageKey; }
}
