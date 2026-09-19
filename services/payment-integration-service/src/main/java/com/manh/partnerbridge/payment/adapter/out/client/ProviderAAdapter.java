package com.manh.partnerbridge.payment.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

public final class ProviderAAdapter extends AbstractPaymentProviderAdapter {
    private final String apiKey;

    public ProviderAAdapter(ObjectMapper mapper, String baseUrl, String apiKey, Duration connectTimeout, Duration responseTimeout) {
        super(mapper, baseUrl, connectTimeout, responseTimeout);
        this.apiKey = apiKey;
    }

    public String providerCode() { return "PROVIDER_A"; }
    protected String path() { return "/v1/payments"; }
    protected Map<String, String> authHeaders() { return Map.of("X-Api-Key", apiKey); }
    protected Object body(CreatePaymentCommand command) {
        return Map.of("merchantRef", command.merchantReference(), "amount", new BigDecimal(command.amount().value()),
            "currency", command.amount().currency(), "note", command.description() == null ? "Payment" : command.description());
    }
    protected ProviderResult mapSuccess(JsonNode node, CreatePaymentCommand command) {
        if (!"SUCCESS".equals(requiredText(node, "code")) || !node.path("amount").isNumber()
            || !node.path("currency").isTextual()
            || node.path("amount").decimalValue().compareTo(new BigDecimal(command.amount().value())) != 0
            || !node.path("currency").asText().equals(command.amount().currency()))
            throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
        return new ProviderResult(requiredText(node, "transactionId"), status(requiredText(node, "paymentStatus"),
            Map.of("COMPLETED", PaymentStatus.SUCCEEDED, "PROCESSING", PaymentStatus.PENDING, "REJECTED", PaymentStatus.FAILED)));
    }
}
