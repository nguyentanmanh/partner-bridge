package com.manh.partnerbridge.payment.adapter.in.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Set;

/** Strict JSON types and additionalProperties checks, scoped to the Payment API only. */
public final class CreatePaymentRequestDeserializer extends JsonDeserializer<CreatePaymentRequest> {
    @Override
    public CreatePaymentRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode root = parser.getCodec().readTree(parser);
        object(root, Set.of("providerCode", "merchantReference", "amount", "description"), context);
        JsonNode money = root.path("amount");
        object(money, Set.of("value", "currency"), context);
        return new CreatePaymentRequest(text(root, "providerCode", context), text(root, "merchantReference", context),
            new CreatePaymentRequest.Amount(text(money, "value", context), text(money, "currency", context)),
            root.has("description") ? text(root, "description", context) : null);
    }

    private static void object(JsonNode node, Set<String> allowed, DeserializationContext context) throws IOException {
        if (!node.isObject()) context.reportInputMismatch(CreatePaymentRequest.class, "Expected JSON object");
        var fields = node.fieldNames();
        while (fields.hasNext()) {
            if (!allowed.contains(fields.next())) {
                context.reportInputMismatch(CreatePaymentRequest.class, "Undeclared property");
            }
        }
    }

    private static String text(JsonNode object, String name, DeserializationContext context) throws IOException {
        JsonNode node = object.path(name);
        if (!node.isTextual()) context.reportInputMismatch(CreatePaymentRequest.class, "Expected JSON string");
        return node.textValue();
    }
}
