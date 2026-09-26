package com.manh.partnerbridge.banking.application.command;

import java.math.BigDecimal;

public record CreateTransferCommand(String fromAccount, String toAccount, BigDecimal amount,
                                    String currency, String reference) {}
