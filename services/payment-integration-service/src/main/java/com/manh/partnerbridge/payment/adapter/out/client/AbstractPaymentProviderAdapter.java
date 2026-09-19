package com.manh.partnerbridge.payment.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.domain.model.PaymentStatus;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractPaymentProviderAdapter implements PaymentProviderPort {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPaymentProviderAdapter.class);
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final Duration responseTimeout;

    AbstractPaymentProviderAdapter(ObjectMapper mapper, String baseUrl, Duration connectTimeout, Duration responseTimeout) {
        this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.responseTimeout = responseTimeout;
    }

    protected abstract String path();
    protected abstract Object body(CreatePaymentCommand command);
    protected abstract Map<String, String> authHeaders();
    protected abstract ProviderResult mapSuccess(JsonNode response, CreatePaymentCommand command);

    @Override
    public final ProviderResult create(CreatePaymentCommand command, String requestId) {
        long started = System.nanoTime();
        String callOutcome = "UNKNOWN";
        String paymentStatus = "UNKNOWN";
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path()))
                .timeout(responseTimeout).header("Content-Type", "application/json")
                .header("Request-ID", requestId)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body(command))));
            authHeaders().forEach(builder::header);
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403)
                throw new PaymentException(PaymentErrorCode.PROVIDER_AUTH);
            if (response.statusCode() == 422)
                throw new PaymentException(PaymentErrorCode.PROVIDER_REJECTED);
            if (response.statusCode() >= 500)
                throw new PaymentException(PaymentErrorCode.PROVIDER_UNAVAILABLE);
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
            JsonNode parsed;
            try {
                parsed = mapper.readTree(response.body());
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
            }
            if (parsed == null) throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
            ProviderResult result = mapSuccess(parsed, command);
            callOutcome = "RESPONSE_ACCEPTED";
            paymentStatus = result.status().name();
            return result;
        } catch (PaymentException ex) {
            callOutcome = switch (ex.errorCode()) {
                case PROVIDER_REJECTED -> "BUSINESS_REJECTION";
                case PROVIDER_AUTH -> "AUTHENTICATION_ERROR";
                case INVALID_PROVIDER_RESPONSE -> "INVALID_RESPONSE";
                case PROVIDER_UNAVAILABLE -> "UNAVAILABLE";
                default -> "UNKNOWN";
            };
            throw ex;
        } catch (HttpTimeoutException ex) {
            callOutcome = "TIMEOUT";
            throw new PaymentException(PaymentErrorCode.PROVIDER_TIMEOUT);
        } catch (ConnectException ex) {
            callOutcome = "UNAVAILABLE";
            throw new PaymentException(PaymentErrorCode.PROVIDER_UNAVAILABLE);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PaymentException(PaymentErrorCode.PROVIDER_UNAVAILABLE);
        } catch (IOException ex) {
            callOutcome = "IO_ERROR";
            throw new PaymentException(PaymentErrorCode.PROVIDER_UNAVAILABLE);
        } finally {
            LOG.info("provider={} requestId={} durationMs={} callOutcome={} paymentStatus={}", providerCode(), requestId,
                Duration.ofNanos(System.nanoTime() - started).toMillis(), callOutcome, paymentStatus);
        }
    }

    protected static String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank())
            throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
        return value.asText();
    }

    protected static PaymentStatus status(String raw, Map<String, PaymentStatus> mapping) {
        PaymentStatus value = mapping.get(raw);
        if (value == null) throw new PaymentException(PaymentErrorCode.INVALID_PROVIDER_RESPONSE);
        return value;
    }
}
