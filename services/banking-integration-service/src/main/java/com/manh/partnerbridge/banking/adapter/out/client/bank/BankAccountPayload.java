package com.manh.partnerbridge.banking.adapter.out.client.bank;

import java.math.BigDecimal;

record BankAccountPayload(String accountNumber, String customerName, BigDecimal availableBalance,
                          String currency, String status) {}
