package com.manh.partnerbridge.payment.adapter.in.rest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonDeserialize(using = CreatePaymentRequestDeserializer.class)
public record CreatePaymentRequest(
    @NotBlank @Pattern(regexp = "PROVIDER_A|PROVIDER_B") String providerCode,
    @NotBlank @Size(max = 100) String merchantReference,
    @NotNull @Valid Amount amount,
    @Size(max = 255) @Pattern(regexp = ".*\\S.*") String description) {
    public record Amount(
        @NotBlank @Size(max = 32) @Pattern(regexp = "^(?:0\\.[0-9]*[1-9][0-9]*|[1-9][0-9]*(?:\\.[0-9]+)?)$") String value,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency) {}
}
