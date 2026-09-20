package com.manh.partnerbridge.payment.adapter.out.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manh.partnerbridge.payment.adapter.out.client.providera.ProviderAAdapter;
import com.manh.partnerbridge.payment.adapter.out.client.providera.ProviderAMapper;
import com.manh.partnerbridge.payment.adapter.out.client.providerb.ProviderBAdapter;
import com.manh.partnerbridge.payment.adapter.out.client.providerb.ProviderBMapper;
import com.manh.partnerbridge.payment.infrastructure.config.http.ProviderClientProperties;
import com.manh.partnerbridge.payment.infrastructure.config.http.ProviderHttpClientConfiguration;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.domain.model.Money;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import com.manh.partnerbridge.payment.domain.model.PaymentStatus;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class PaymentProviderAdapterTest {
    private HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProviderHttpClientConfiguration configuration = new ProviderHttpClientConfiguration();
    private final AtomicReference<String> apiKey = new AtomicReference<>();
    private final AtomicReference<String> signature = new AtomicReference<>();
    private final AtomicReference<String> clientId = new AtomicReference<>();
    private final CreatePaymentCommand command = new CreatePaymentCommand("PROVIDER_A", "ORDER-SUCCESS-001",
        new Money("100000", "VND"), "Test payment");

    private String start(int status, String response, long delayMs, AtomicReference<String> requestBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiKey.set(exchange.getRequestHeaders().getFirst("X-Api-Key"));
            signature.set(exchange.getRequestHeaders().getFirst("X-Signature"));
            clientId.set(exchange.getRequestHeaders().getFirst("X-Client-Id"));
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.sendResponseHeaders(status, bytes.length);
                try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
            } catch (java.io.IOException ignored) { /* Client can time out before this response. */ }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach void stop() { if (server != null) server.stop(0); }

    private ProviderAAdapter a(String url, String key, Duration connectTimeout, Duration responseTimeout) {
        var properties = new ProviderClientProperties.ProviderA(url, key, connectTimeout, responseTimeout);
        return new ProviderAAdapter(configuration.providerAHttpClient(properties), new ProviderAMapper(mapper),
            configuration.providerCallSupport(), properties.apiKey());
    }

    private ProviderBAdapter b(String url, String id, String signed, Duration connectTimeout, Duration responseTimeout) {
        var properties = new ProviderClientProperties.ProviderB(url, id, signed, connectTimeout, responseTimeout);
        return new ProviderBAdapter(configuration.providerBHttpClient(properties), new ProviderBMapper(mapper),
            configuration.providerCallSupport(), properties.clientId(), properties.signature());
    }

    @Test void providerAMapsNumericAmountAndCompletedStatus(CapturedOutput output) throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        String url = start(201, "{\"code\":\"SUCCESS\",\"transactionId\":\"PA-1\",\"paymentStatus\":\"COMPLETED\",\"amount\":100000,\"currency\":\"VND\"}", 0, body);
        var adapter = a(url, "local-key", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentStatus.SUCCEEDED, adapter.create(command, "req-1").status());
        assertTrue(body.get().contains("\"merchantRef\":\"ORDER-SUCCESS-001\""));
        assertTrue(body.get().contains("\"amount\":100000"));
        assertEquals("local-key", apiKey.get());
        assertFalse(output.getOut().contains("local-key"));
    }

    @Test void providerBMapsNestedStringAmountAndPendingStatus(CapturedOutput output) throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        String url = start(200, "{\"responseCode\":\"00\",\"data\":{\"referenceNo\":\"PB-1\",\"status\":\"P\",\"amount\":\"100000\",\"currencyCode\":\"VND\"}}", 0, body);
        var adapter = b(url, "local-client", "local-signature", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentStatus.PENDING, adapter.create(command, "req-1").status());
        assertTrue(body.get().contains("\"orderRef\":\"ORDER-SUCCESS-001\""));
        assertTrue(body.get().contains("\"amount\":\"100000\""));
        assertEquals("local-client", clientId.get());
        assertEquals("local-signature", signature.get());
        assertFalse(output.getOut().contains("local-signature"));
        assertFalse(output.getOut().contains("local-client"));
    }

    @Test void providerBusinessRejectionMapsTo422() throws Exception {
        String url = start(422, "{\"error\":\"PAYMENT_REJECTED\"}", 0, new AtomicReference<>());
        var adapter = b(url, "local-client", "local-signature", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentErrorCode.PROVIDER_REJECTED, assertThrows(PaymentException.class, () -> adapter.create(command, "req")).errorCode());
    }

    @Test void providerAuthenticationMapsTo502() throws Exception {
        String url = start(401, "{\"code\":\"UNAUTHORIZED\"}", 0, new AtomicReference<>());
        var adapter = a(url, "wrong-key", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentErrorCode.PROVIDER_AUTH, assertThrows(PaymentException.class, () -> adapter.create(command, "req")).errorCode());
    }

    @Test void malformedProviderResponseMapsTo502(CapturedOutput output) throws Exception {
        String url = start(200, "{\"responseCode\":\"00\",\"data\":{}}", 0, new AtomicReference<>());
        var adapter = b(url, "local-client", "local-signature", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentErrorCode.INVALID_PROVIDER_RESPONSE, assertThrows(PaymentException.class, () -> adapter.create(command, "req")).errorCode());
        assertTrue(output.getOut().contains("callOutcome=INVALID_RESPONSE paymentStatus=UNKNOWN"));
    }

    @Test void providerTimeoutMapsTo504(CapturedOutput output) throws Exception {
        String url = start(200, "{}", 300, new AtomicReference<>());
        var adapter = a(url, "local-key", Duration.ofMillis(100), Duration.ofMillis(100));
        assertEquals(PaymentErrorCode.PROVIDER_TIMEOUT, assertThrows(PaymentException.class, () -> adapter.create(command, "req")).errorCode());
        assertTrue(output.getOut().contains("callOutcome=TIMEOUT paymentStatus=UNKNOWN"));
        assertFalse(output.getOut().contains("paymentStatus=FAILED"));
    }

    @Test void confirmedRejectedStatusesMapToFailedWithoutChangingFailScenario() throws Exception {
        assertEquals(PaymentStatus.FAILED, new ProviderAMapper(mapper).response(
            "{\"code\":\"SUCCESS\",\"transactionId\":\"PA-1\",\"paymentStatus\":\"REJECTED\",\"amount\":100000,\"currency\":\"VND\"}", command).status());
        assertEquals(PaymentStatus.FAILED, new ProviderBMapper(mapper).response(
            "{\"responseCode\":\"00\",\"data\":{\"referenceNo\":\"PB-1\",\"status\":\"F\",\"amount\":\"100000\",\"currencyCode\":\"VND\"}}", command).status());
    }

    @Test void malformedProviderTypesAreRejected() {
        assertEquals(PaymentErrorCode.INVALID_PROVIDER_RESPONSE, assertThrows(PaymentException.class,
            () -> new ProviderAMapper(mapper).response(
                "{\"code\":\"SUCCESS\",\"transactionId\":\"PA-1\",\"paymentStatus\":\"COMPLETED\",\"amount\":\"100000\",\"currency\":\"VND\"}", command)).errorCode());
        assertEquals(PaymentErrorCode.INVALID_PROVIDER_RESPONSE, assertThrows(PaymentException.class,
            () -> new ProviderBMapper(mapper).response(
                "{\"responseCode\":\"00\",\"data\":{\"referenceNo\":\"PB-1\",\"status\":\"P\",\"amount\":100000,\"currencyCode\":\"VND\"}}", command)).errorCode());
    }

    @Test void invalidTimeoutConfigurationFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> new ProviderClientProperties.ProviderA(
            "http://localhost", "key", Duration.ZERO, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new ProviderClientProperties.ProviderB(
            "http://localhost", "id", "signature", Duration.ofSeconds(1), Duration.ZERO));
    }
}
