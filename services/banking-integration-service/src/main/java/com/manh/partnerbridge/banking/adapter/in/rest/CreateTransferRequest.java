package com.manh.partnerbridge.banking.adapter.in.rest;

import com.manh.partnerbridge.banking.application.command.CreateTransferCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreateTransferRequest(
        @NotBlank String fromAccount,
        @NotBlank String toAccount,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotBlank String reference) {
    CreateTransferCommand toCommand() {
        return new CreateTransferCommand(fromAccount, toAccount, amount, currency, reference);
    }
}
