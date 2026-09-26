package com.manh.partnerbridge.banking.domain.model;

import java.math.BigDecimal;

public record BankAccount(String accountId, String holderName, BigDecimal balance, String currency, String status) {}
