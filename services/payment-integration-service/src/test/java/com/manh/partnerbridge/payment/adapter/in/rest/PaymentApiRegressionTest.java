package com.manh.partnerbridge.payment.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.command.PaymentRequestContext;
import com.manh.partnerbridge.payment.application.port.in.PaymentUseCase;
import com.manh.partnerbridge.payment.bootstrap.Application;
import com.manh.partnerbridge.payment.domain.model.Money;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class PaymentApiRegressionTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ConcurrentHashMap<String, AtomicInteger> CALLS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Gate> GATES = new ConcurrentHashMap<>();
    private static final java.util.concurrent.ExecutorService SERVER_THREADS = Executors.newCachedThreadPool();
    private static HttpServer server;
    @Autowired MockMvc mvc;
    @Autowired Environment environment;
    @Autowired PaymentUseCase useCase;

    @DynamicPropertySource
    static void providers(DynamicPropertyRegistry properties) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(SERVER_THREADS);
        server.createContext("/", exchange -> {
            var request = JSON.readTree(exchange.getRequestBody());
            boolean a = exchange.getRequestURI().getPath().equals("/v1/payments");
            String reference = a ? request.path("merchantRef").asText() : request.path("request").path("orderRef").asText();
            CALLS.computeIfAbsent(reference, ignored -> new AtomicInteger()).incrementAndGet();
            Gate gate = GATES.get(reference);
            if (gate != null) {
                gate.entered.countDown();
                try {
                    if (!gate.release.await(10, TimeUnit.SECONDS)) throw new AssertionError("Provider gate was not released");
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
            int status = reference.contains("FAIL") ? 422 : 200;
            String response;
            if (reference.contains("MALFORMED")) response = "{}";
            else if (status == 422) response = "{\"error\":\"LOCAL_BUSINESS_REJECTION\"}";
            else if (a) response = "{\"code\":\"SUCCESS\",\"transactionId\":\"PA-1\",\"paymentStatus\":\"COMPLETED\",\"amount\":100000,\"currency\":\"VND\"}";
            else response = "{\"responseCode\":\"00\",\"data\":{\"referenceNo\":\"PB-1\",\"status\":\"S\",\"amount\":\"100000\",\"currencyCode\":\"VND\"}}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (java.io.IOException expectedAfterClientTimeout) {
                // The caller may already have timed out. The provider request has still been counted.
            } finally { exchange.close(); }
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        properties.add("partnerbridge.provider-a.base-url", () -> url);
        properties.add("partnerbridge.provider-b.base-url", () -> url);
        properties.add("partnerbridge.provider-a.response-timeout", () -> "2s");
        properties.add("partnerbridge.provider-b.response-timeout", () -> "2s");
    }

    @AfterAll static void stopServer() {
        GATES.values().forEach(g -> g.release.countDown());
        if (server != null) server.stop(0);
        SERVER_THREADS.shutdownNow();
    }

    private ObjectNode body(String provider, String reference) {
        ObjectNode body = JSON.createObjectNode();
        body.put("providerCode", provider).put("merchantReference", reference).put("description", "Local test");
        body.putObject("amount").put("value", "100000").put("currency", "VND");
        return body;
    }

    private MvcResult create(ObjectNode body, String key, String requestId, String language) throws Exception {
        return mvc.perform(post("/api/v1/payments").contentType("application/json")
            .header("Request-ID", requestId).header("Idempotency-Key", key).header("Accept-Language", language)
            .content(JSON.writeValueAsBytes(body))).andReturn();
    }

    private void assertCode(MvcResult result, int status, String code) throws Exception {
        assertEquals(status, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        assertEquals(code, JSON.readTree(result.getResponse().getContentAsByteArray()).path("code").asText());
    }

    @ParameterizedTest
    @CsvSource({"PROVIDER_A,TIMEOUT,504", "PROVIDER_B,TIMEOUT,504",
        "PROVIDER_A,MALFORMED,502", "PROVIDER_B,MALFORMED,502", "PROVIDER_A,FAIL,422", "PROVIDER_B,FAIL,422"})
    void errorReplayIsExactSequentiallyAndConcurrently(String provider, String scenario, int expected) throws Exception {
        String reference = scenario + "-" + UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        ObjectNode body = body(provider, reference);
        Gate gate = new Gate();
        GATES.put(reference, gate);
        try (var executor = Executors.newFixedThreadPool(8)) {
            try {
                var first = executor.submit(() -> create(body, key, "original", "en"));
                assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
                var waiters = new ArrayList<java.util.concurrent.Future<MvcResult>>();
                for (int i = 0; i < 4; i++) {
                    int id = i;
                    waiters.add(executor.submit(() -> create(body, key, "waiter-" + id, "vi")));
                }
                ObjectNode changed = body.deepCopy().put("description", "Different payload");
                assertCode(executor.submit(() -> create(changed, key, "conflict", "en")).get(1, TimeUnit.SECONDS),
                    409, "PAY-IDEMPOTENCY-001");
                assertCode(executor.submit(() -> create(body, "different-" + key, "reference-conflict", "en")).get(1, TimeUnit.SECONDS),
                    409, "PAY-PAYMENT-002");
                if (!scenario.equals("TIMEOUT")) gate.release.countDown();
                MvcResult original = first.get(5, TimeUnit.SECONDS);
                assertEquals(expected, original.getResponse().getStatus());
                byte[] originalBody = original.getResponse().getContentAsByteArray();
                for (var waiter : waiters) {
                    var response = waiter.get(5, TimeUnit.SECONDS).getResponse();
                    assertEquals(expected, response.getStatus());
                    assertArrayEquals(originalBody, response.getContentAsByteArray());
                }
                var replay = create(body, key, "replay", "vi").getResponse();
                assertEquals(expected, replay.getStatus());
                assertArrayEquals(originalBody, replay.getContentAsByteArray());
                assertEquals("replay", replay.getHeader("Request-ID"));
                assertEquals("original", JSON.readTree(replay.getContentAsByteArray()).path("requestId").asText());
                assertCode(create(body, "another-" + key, "after-error-conflict", "en"), 409, "PAY-PAYMENT-002");
                assertEquals(1, CALLS.get(reference).get());
            } finally { gate.release.countDown(); }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROVIDER_A", "PROVIDER_B"})
    void successReplayAndDefaultClientBinding(String provider) throws Exception {
        String reference = "SUCCESS-" + UUID.randomUUID();
        String key = UUID.randomUUID().toString();
        ObjectNode body = body(provider, reference);
        var first = create(body, key, "first", "en").getResponse();
        var replay = mvc.perform(post("/api/v1/payments").contentType("application/json")
            .header("Request-ID", "replay").header("Idempotency-Key", key).header("X-Client-Id", "untrusted-local-value")
            .content(JSON.writeValueAsBytes(body))).andReturn().getResponse();
        assertEquals(201, first.getStatus());
        assertEquals(201, replay.getStatus());
        assertArrayEquals(first.getContentAsByteArray(), replay.getContentAsByteArray());
        assertEquals("SUCCEEDED", JSON.readTree(first.getContentAsByteArray()).path("status").asText());
        assertEquals("local-poc-client", environment.getProperty("partnerbridge.client.default-id"));
        var same = useCase.create(new CreatePaymentCommand(provider, reference, new Money("100000", "VND"), "Local test"),
            "local-poc-client", key, new PaymentRequestContext("direct", "trace", Locale.ENGLISH));
        assertEquals(same.paymentId(), JSON.readTree(first.getContentAsByteArray()).path("paymentId").asText());
        assertEquals(1, CALLS.get(reference).get());
    }

    @Test void invalidCanonicalJsonNeverCallsProvider() throws Exception {
        for (String variant : List.of("number", "amount-extra", "callback", "null-description", "boolean", "unknown-provider", "zero")) {
            String reference = "SUCCESS-" + UUID.randomUUID();
            ObjectNode body = body("PROVIDER_A", reference);
            switch (variant) {
                case "number" -> ((ObjectNode) body.get("amount")).put("value", 100000);
                case "amount-extra" -> ((ObjectNode) body.get("amount")).put("extra", "invalid");
                case "callback" -> body.put("callbackUrl", "http://localhost/unused-local-test");
                case "null-description" -> body.putNull("description");
                case "boolean" -> ((ObjectNode) body.get("amount")).put("value", true);
                case "unknown-provider" -> body.put("providerCode", "UNKNOWN");
                case "zero" -> ((ObjectNode) body.get("amount")).put("value", "0");
                default -> throw new AssertionError(variant);
            }
            assertCode(create(body, UUID.randomUUID().toString(), "validation", "en"), 400, "PAY-VALIDATION-001");
            assertFalse(CALLS.containsKey(reference), variant);
        }
    }

    @Test void independentHttpRequestAndGetAreNotBlocked() throws Exception {
        String existingRef = "SUCCESS-" + UUID.randomUUID();
        var existing = create(body("PROVIDER_A", existingRef), existingRef, "existing", "en");
        String id = JSON.readTree(existing.getResponse().getContentAsByteArray()).path("paymentId").asText();
        String slowRef = "TIMEOUT-" + UUID.randomUUID();
        Gate gate = new Gate();
        GATES.put(slowRef, gate);
        try (var executor = Executors.newFixedThreadPool(4)) {
            try {
                var slow = executor.submit(() -> create(body("PROVIDER_A", slowRef), slowRef, "slow", "en"));
                assertTrue(gate.entered.await(5, TimeUnit.SECONDS));
                var get = executor.submit(() -> mvc.perform(get("/api/v1/payments/" + id).header("Request-ID", "get")).andReturn());
                assertEquals(200, get.get(1, TimeUnit.SECONDS).getResponse().getStatus());
                String fastRef = "SUCCESS-" + UUID.randomUUID();
                assertEquals(201, executor.submit(() -> create(body("PROVIDER_B", fastRef), fastRef, "fast", "en"))
                    .get(1, TimeUnit.SECONDS).getResponse().getStatus());
                assertFalse(slow.isDone());
                assertEquals(504, slow.get(5, TimeUnit.SECONDS).getResponse().getStatus());
            } finally { gate.release.countDown(); }
        }
    }

    private static final class Gate {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
    }
}
