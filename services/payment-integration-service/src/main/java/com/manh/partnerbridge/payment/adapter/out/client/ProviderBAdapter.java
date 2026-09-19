package com.manh.partnerbridge.payment.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.domain.model.PaymentStatus;
import java.time.Duration;
import java.util.Map;

public final class ProviderBAdapter extends AbstractPaymentProviderAdapter {
    private final String clientId;
    private final String signature;

    public ProviderBAdapter(ObjectMapper mapper, String baseUrl, String clientId, String signature,
                            Duration connectTimeout, Duration responseTimeout) {
        super(mapper, baseUrl, connectTimeout, responseTimeout);
        this.clientId = clientId;
        this.signature = signature;
    }

    public String providerCode() { return "PROVIDER_B"; }
    protected String path() { return "/api/payment/create"; }
    protected Map<String, String> authHeaders() {
        return Map.of("X-Client-Id", clientId, "X-Signature", signature);
    }
    protected Object body(CreatePaymentCommand command) {
        return Map.of("request", Map.of("orderRef", command.merchantReference(),
            "money", Map.of("amount", command.amount().value(), "currencyCode", command.amount().currency()),
            "content", command.description() == null ? "Payment" : command.description()));
    }
    protected ProviderResult mapSuccess(JsonNode node, CreatePaymentCommand command) {
        if (!"00".equals(requiredText(node, "responseCode")))
            throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
        JsonNode data = node.path("data");
        if (!requiredText(data, "amount").equals(command.amount().value())
            || !requiredText(data, "currencyCode").equals(command.amount().currency()))
            throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
        return new ProviderResult(requiredText(data, "referenceNo"), status(requiredText(data, "status"),
            Map.of("S", PaymentStatus.SUCCEEDED, "P", PaymentStatus.PENDING, "F", PaymentStatus.FAILED)));
    }
}
