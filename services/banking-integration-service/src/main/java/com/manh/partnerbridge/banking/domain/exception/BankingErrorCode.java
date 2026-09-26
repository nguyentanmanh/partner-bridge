package com.manh.partnerbridge.banking.domain.exception;

public enum BankingErrorCode implements ErrorCode {
    ACCOUNT_NOT_FOUND("BNK-ACCOUNT-001", "error.account.not-found"),
    TRANSFER_REJECTED("BNK-TRANSFER-001", "error.transfer.rejected");

    private final String code;
    private final String messageKey;
    BankingErrorCode(String code, String messageKey) { this.code = code; this.messageKey = messageKey; }
    public String code() { return code; }
    public String messageKey() { return messageKey; }
}
