package com.manh.partnerbridge.payment.adapter.out.client.providerb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort.ProviderResult;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.domain.model.PaymentStatus;
import java.util.Map;

public final class ProviderBMapper {
    private static final Map<String, PaymentStatus> STATUSES = Map.of(
        "S", PaymentStatus.SUCCEEDED, "P", PaymentStatus.PENDING, "F", PaymentStatus.FAILED);
    private final ObjectMapper json;

    public ProviderBMapper(ObjectMapper json) { this.json = json; }

    public ProviderBRequest request(CreatePaymentCommand command) {
        return new ProviderBRequest(new ProviderBRequest.Details(command.merchantReference(),
            new ProviderBRequest.Money(command.amount().value(), command.amount().currency()),
            command.description() == null ? "Payment" : command.description()));
    }

    public ProviderResult response(String raw, CreatePaymentCommand command) {
        try {
            JsonNode node = json.readTree(raw);
            if (node == null || !node.path("responseCode").isTextual()) throw invalid();
            JsonNode data = node.path("data");
            if (!data.isObject() || !data.path("referenceNo").isTextual() || !data.path("status").isTextual()
                || !data.path("amount").isTextual() || !data.path("currencyCode").isTextual()) throw invalid();
            ProviderBResponse response = json.treeToValue(node, ProviderBResponse.class);
            if (!"00".equals(response.responseCode()) || response.data().referenceNo().isBlank()
                || !command.amount().value().equals(response.data().amount())
                || !command.amount().currency().equals(response.data().currencyCode())) throw invalid();
            PaymentStatus status = STATUSES.get(response.data().status());
            if (status == null) throw invalid();
            return new ProviderResult(response.data().referenceNo(), status);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw invalid();
        }
    }

    private static PaymentException invalid() { return new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE); }
}
