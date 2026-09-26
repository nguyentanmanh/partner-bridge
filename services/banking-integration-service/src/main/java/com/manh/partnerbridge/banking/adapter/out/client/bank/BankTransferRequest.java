package com.manh.partnerbridge.banking.adapter.out.client.bank;

import java.math.BigDecimal;

record BankTransferRequest(String debitAccount, String creditAccount, BigDecimal amount,
                           String currency, String clientReference) {}
