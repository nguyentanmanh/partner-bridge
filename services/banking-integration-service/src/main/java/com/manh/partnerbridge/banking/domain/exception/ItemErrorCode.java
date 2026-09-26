package com.manh.partnerbridge.banking.domain.exception;

public enum ItemErrorCode implements ErrorCode {
    NOT_FOUND("BNK-ITEM-001", "error.item.not-found");
    private final String code; private final String messageKey;
    ItemErrorCode(String code, String messageKey) { this.code = code; this.messageKey = messageKey; }
    public String code() { return code; } public String messageKey() { return messageKey; }
}
