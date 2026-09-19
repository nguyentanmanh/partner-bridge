package com.manh.partnerbridge.payment.adapter.out.client;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final CreatePaymentCommand command = new CreatePaymentCommand("PROVIDER_A", "ORDER-SUCCESS-001",
        new Money("100000", "VND"), "Test payment");

    private String start(int status, String response, long delayMs, AtomicReference<String> requestBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach void stop() { if (server != null) server.stop(0); }

    @Test void providerAMapsNumericAmountAndCompletedStatus() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        String url = start(201, "{\"code\":\"SUCCESS\",\"transactionId\":\"PA-1\",\"paymentStatus\":\"COMPLETED\",\"amount\":100000,\"currency\":\"VND\"}", 0, body);
        var adapter = new ProviderAAdapter(mapper, url, "local-key", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentStatus.SUCCEEDED, adapter.create(command, "req-1").status());
        assertTrue(body.get().contains("\"merchantRef\":\"ORDER-SUCCESS-001\""));
        assertTrue(body.get().contains("\"amount\":100000"));
    }

    @Test void providerBMapsNestedStringAmountAndPendingStatus() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        String url = start(200, "{\"responseCode\":\"00\",\"data\":{\"referenceNo\":\"PB-1\",\"status\":\"P\",\"amount\":\"100000\",\"currencyCode\":\"VND\"}}", 0, body);
        var adapter = new ProviderBAdapter(mapper, url, "local-client", "local-signature", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentStatus.PENDING, adapter.create(command, "req-1").status());
        assertTrue(body.get().contains("\"orderRef\":\"ORDER-SUCCESS-001\""));
        assertTrue(body.get().contains("\"amount\":\"100000\""));
    }

    @Test void providerBusinessRejectionMapsTo422() throws Exception {
        String url = start(422, "{\"error\":\"PAYMENT_REJECTED\"}", 0, new AtomicReference<>());
        var adapter = new ProviderBAdapter(mapper, url, "local-client", "local-signature", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentErrorCode.PROVIDER_REJECTED, assertThrows(PaymentException.class, () -> adapter.create(command, "req")).errorCode());
    }

    @Test void providerAuthenticationMapsTo502() throws Exception {
        String url = start(401, "{\"code\":\"UNAUTHORIZED\"}", 0, new AtomicReference<>());
        var adapter = new ProviderAAdapter(mapper, url, "wrong-key", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentErrorCode.PROVIDER_AUTH, assertThrows(PaymentException.class, () -> adapter.create(command, "req")).errorCode());
    }

    @Test void malformedProviderResponseMapsTo502(CapturedOutput output) throws Exception {
        String url = start(200, "{\"responseCode\":\"00\",\"data\":{}}", 0, new AtomicReference<>());
        var adapter = new ProviderBAdapter(mapper, url, "local-client", "local-signature", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentErrorCode.INVALID_PROVIDER_RESPONSE, assertThrows(PaymentException.class, () -> adapter.create(command, "req")).errorCode());
        assertTrue(output.getOut().contains("callOutcome=INVALID_RESPONSE paymentStatus=UNKNOWN"));
    }

    @Test void providerTimeoutMapsTo504(CapturedOutput output) throws Exception {
        String url = start(200, "{}", 300, new AtomicReference<>());
        var adapter = new ProviderAAdapter(mapper, url, "local-key", Duration.ofMillis(100), Duration.ofMillis(100));
        assertEquals(PaymentErrorCode.PROVIDER_TIMEOUT, assertThrows(PaymentException.class, () -> adapter.create(command, "req")).errorCode());
        assertTrue(output.getOut().contains("callOutcome=TIMEOUT paymentStatus=UNKNOWN"));
        assertFalse(output.getOut().contains("paymentStatus=FAILED"));
    }

    @Test void confirmedRejectedStatusesMapToFailedWithoutChangingFailScenario() throws Exception {
        var a = new ProviderAAdapter(mapper, "http://localhost", "local-key", Duration.ofSeconds(1), Duration.ofSeconds(1));
        var b = new ProviderBAdapter(mapper, "http://localhost", "local-client", "local-signature", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertEquals(PaymentStatus.FAILED, a.mapSuccess(mapper.readTree(
            "{\"code\":\"SUCCESS\",\"transactionId\":\"PA-1\",\"paymentStatus\":\"REJECTED\",\"amount\":100000,\"currency\":\"VND\"}"), command).status());
        assertEquals(PaymentStatus.FAILED, b.mapSuccess(mapper.readTree(
            "{\"responseCode\":\"00\",\"data\":{\"referenceNo\":\"PB-1\",\"status\":\"F\",\"amount\":\"100000\",\"currencyCode\":\"VND\"}}"), command).status());
    }
}
