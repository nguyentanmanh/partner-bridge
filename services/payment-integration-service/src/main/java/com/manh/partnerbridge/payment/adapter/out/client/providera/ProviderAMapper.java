package com.manh.partnerbridge.payment.adapter.out.client.providera;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort.ProviderResult;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.util.Map;

public final class ProviderAMapper {
    private static final Map<String, PaymentStatus> STATUSES = Map.of(
        "COMPLETED", PaymentStatus.SUCCEEDED, "PROCESSING", PaymentStatus.PENDING, "REJECTED", PaymentStatus.FAILED);
    private final ObjectMapper json;

    public ProviderAMapper(ObjectMapper json) { this.json = json; }

    public ProviderARequest request(CreatePaymentCommand command) {
        return new ProviderARequest(command.merchantReference(), new BigDecimal(command.amount().value()),
            command.amount().currency(), command.description() == null ? "Payment" : command.description());
    }

    public ProviderResult response(String raw, CreatePaymentCommand command) {
        try {
            JsonNode node = json.readTree(raw);
            if (node == null || !node.path("amount").isNumber() || !node.path("currency").isTextual()
                || !node.path("code").isTextual() || !node.path("transactionId").isTextual()
                || !node.path("paymentStatus").isTextual()) throw invalid();
            ProviderAResponse response = json.treeToValue(node, ProviderAResponse.class);
            if (!"SUCCESS".equals(response.code()) || blank(response.transactionId())
                || response.amount().compareTo(new BigDecimal(command.amount().value())) != 0
                || !command.amount().currency().equals(response.currency())) throw invalid();
            PaymentStatus status = STATUSES.get(response.paymentStatus());
            if (status == null) throw invalid();
            return new ProviderResult(response.transactionId(), status);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static PaymentException invalid() { return new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE); }
}
