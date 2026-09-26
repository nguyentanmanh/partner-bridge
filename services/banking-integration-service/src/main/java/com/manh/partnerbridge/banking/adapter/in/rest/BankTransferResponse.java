package com.manh.partnerbridge.banking.adapter.in.rest;

import com.manh.partnerbridge.banking.domain.model.BankTransfer;

public record BankTransferResponse(String transferId, String status) {
    static BankTransferResponse from(BankTransfer transfer) {
        return new BankTransferResponse(transfer.transferId(), transfer.status());
    }
}
