package com.manh.partnerbridge.banking.adapter.out.client.bank;

import java.util.List;

public record BankTransactionsPayload(List<BankTransactionPayload> transactions) {}
