package com.manh.partnerbridge.payment.application.usecase;

import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.command.PaymentRequestContext;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.domain.model.Money;
import com.manh.partnerbridge.payment.domain.model.PaymentStatus;
import com.manh.partnerbridge.payment.support.MutableClock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {
    private final AtomicInteger calls = new AtomicInteger();
    private final MutableClock clock = new MutableClock();

    private PaymentService service(Function<CreatePaymentCommand, PaymentProviderPort.ProviderResult> behavior) {
        PaymentProviderPort provider = new PaymentProviderPort() {
            public String providerCode() { return "PROVIDER_A"; }
            public ProviderResult create(CreatePaymentCommand command, String requestId) {
                calls.incrementAndGet();
                return behavior.apply(command);
            }
        };
        return new PaymentService(List.of(provider), clock, Duration.ofHours(24),
            (code, locale) -> locale.getLanguage() + ":" + code.code());
    }
    private PaymentProviderPort.ProviderResult success() {
        return new PaymentProviderPort.ProviderResult("PA-TXN-001", PaymentStatus.SUCCEEDED);
    }
    private CreatePaymentCommand command(String reference) {
        return new CreatePaymentCommand("PROVIDER_A", reference, new Money("100000", "VND"), "Test");
    }
    private PaymentRequestContext context(String id) { return new PaymentRequestContext(id, "trace-" + id, Locale.ENGLISH); }
    private void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException ex) { throw new AssertionError(ex); }
    }

    @Test void replayReturnsSameResultAndConflictsNeverCallProvider() {
        var service = service(c -> success());
        var first = service.create(command("ORDER-1"), "client", "key", context("first"));
        assertEquals(first, service.create(command("ORDER-1"), "client", "key", context("replay")));
        assertEquals(first, service.get(first.paymentId()));
        assertEquals(PaymentErrorCode.IDEMPOTENCY_CONFLICT, assertThrows(PaymentException.class,
            () -> service.create(command("ORDER-2"), "client", "key", context("conflict"))).errorCode());
        assertEquals(PaymentErrorCode.DUPLICATE_REFERENCE, assertThrows(PaymentException.class,
            () -> service.create(command("ORDER-1"), "client", "another-key", context("conflict"))).errorCode());
        assertEquals(1, calls.get());
    }

    @ParameterizedTest
    @EnumSource(value = PaymentErrorCode.class, names = {"PROVIDER_TIMEOUT", "INVALID_PROVIDER_RESPONSE", "PROVIDER_REJECTED",
        "PROVIDER_AUTH", "PROVIDER_UNAVAILABLE", "INTERNAL_ERROR"})
    void sequentialAndConcurrentErrorsAreRecordedOnce(PaymentErrorCode error) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var service = service(c -> {
            entered.countDown();
            await(release);
            if (error == PaymentErrorCode.INTERNAL_ERROR) throw new IllegalStateException("sensitive detail");
            throw new PaymentException(error);
        });
        try (var executor = Executors.newFixedThreadPool(12)) {
            try {
                var first = executor.submit(() -> assertThrows(PaymentException.class,
                    () -> service.create(command("ORDER-1"), "client", "key", context("first"))));
                await(entered);
                var waiters = new ArrayList<java.util.concurrent.Future<PaymentException>>();
                for (int i = 0; i < 6; i++) {
                    int id = i;
                    waiters.add(executor.submit(() -> assertThrows(PaymentException.class,
                        () -> service.create(command("ORDER-1"), "client", "key", context("waiter-" + id)))));
                }
                // Conflicts must fail while the first provider request is still in flight.
                assertEquals(PaymentErrorCode.IDEMPOTENCY_CONFLICT, executor.submit(() -> assertThrows(PaymentException.class,
                    () -> service.create(command("ORDER-2"), "client", "key", context("conflict"))).errorCode()).get(1, TimeUnit.SECONDS));
                assertEquals(PaymentErrorCode.DUPLICATE_REFERENCE, executor.submit(() -> assertThrows(PaymentException.class,
                    () -> service.create(command("ORDER-1"), "client", "other-key", context("conflict"))).errorCode()).get(1, TimeUnit.SECONDS));
                release.countDown();
                var recorded = first.get(2, TimeUnit.SECONDS).recordedFailure();
                assertEquals(error, recorded.code());
                assertEquals("first", recorded.requestId());
                assertFalse(recorded.message().contains("sensitive"));
                for (var waiter : waiters) assertEquals(recorded, waiter.get(2, TimeUnit.SECONDS).recordedFailure());
                var replay = assertThrows(PaymentException.class, () -> service.create(command("ORDER-1"), "client", "key",
                    new PaymentRequestContext("replay", "different-trace", Locale.forLanguageTag("vi"))));
                assertEquals(recorded, replay.recordedFailure());
                assertEquals(PaymentErrorCode.DUPLICATE_REFERENCE, assertThrows(PaymentException.class,
                    () -> service.create(command("ORDER-1"), "client", "other-key", context("unknown-conflict"))).errorCode());
                assertEquals(1, calls.get());
            } finally { release.countDown(); }
        }
    }

    @Test void independentCreateAndGetDoNotWaitForSlowProvider() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var service = service(c -> {
            if (c.merchantReference().equals("SLOW")) { entered.countDown(); await(release); }
            return success();
        });
        var existing = service.create(command("EXISTING"), "client", "existing", context("existing"));
        try (var executor = Executors.newFixedThreadPool(4)) {
            try {
                var slow = executor.submit(() -> service.create(command("SLOW"), "client", "slow", context("slow")));
                await(entered);
                assertEquals(existing, executor.submit(() -> service.get(existing.paymentId())).get(1, TimeUnit.SECONDS));
                assertNotNull(executor.submit(() -> service.create(command("FAST"), "client", "fast", context("fast"))).get(1, TimeUnit.SECONDS));
                assertFalse(slow.isDone());
                release.countDown();
                assertNotNull(slow.get(2, TimeUnit.SECONDS));
            } finally { release.countDown(); }
        }
    }

    @Test void inFlightDoesNotExpireAndTtlStartsAtCompletion() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var service = service(c -> { entered.countDown(); await(release); return success(); });
        try (var executor = Executors.newFixedThreadPool(3)) {
            try {
                var first = executor.submit(() -> service.create(command("ORDER-1"), "client", "key", context("first")));
                await(entered);
                clock.advance(Duration.ofDays(2));
                // This create executes cleanup, but must still find the IN_FLIGHT reservation.
                assertEquals(PaymentErrorCode.IDEMPOTENCY_CONFLICT, executor.submit(() -> assertThrows(PaymentException.class,
                    () -> service.create(command("ORDER-2"), "client", "key", context("conflict"))).errorCode()).get(1, TimeUnit.SECONDS));
                var waiter = executor.submit(() -> service.create(command("ORDER-1"), "client", "key", context("waiter")));
                release.countDown();
                var payment = first.get(2, TimeUnit.SECONDS);
                assertEquals(payment, waiter.get(2, TimeUnit.SECONDS));
                clock.advance(Duration.ofHours(24).minusNanos(1));
                assertEquals(payment, service.create(command("ORDER-1"), "client", "key", context("replay")));
                clock.advance(Duration.ofNanos(1));
                assertEquals(PaymentErrorCode.DUPLICATE_REFERENCE, assertThrows(PaymentException.class,
                    () -> service.create(command("ORDER-1"), "client", "key", context("expired"))).errorCode());
                assertEquals(1, calls.get());
                service.create(command("NEW-REFERENCE"), "client", "key", context("new"));
                assertEquals(2, calls.get());
            } finally { release.countDown(); }
        }
    }

    @ParameterizedTest
    @EnumSource(value = PaymentErrorCode.class, names = {"PROVIDER_TIMEOUT", "INVALID_PROVIDER_RESPONSE", "PROVIDER_REJECTED"})
    void expiredErrorResponseNeverReleasesMerchantReference(PaymentErrorCode code) {
        var service = service(c -> { throw new PaymentException(code); });
        var failure = assertThrows(PaymentException.class, () -> service.create(command("ORDER-1"), "client", "key", context("first")));
        clock.advance(Duration.ofHours(23));
        assertEquals(failure.recordedFailure(), assertThrows(PaymentException.class,
            () -> service.create(command("ORDER-1"), "client", "key", context("replay"))).recordedFailure());
        clock.advance(Duration.ofHours(1));
        assertEquals(PaymentErrorCode.DUPLICATE_REFERENCE, assertThrows(PaymentException.class,
            () -> service.create(command("ORDER-1"), "client", "key", context("expired"))).errorCode());
        assertEquals(PaymentErrorCode.DUPLICATE_REFERENCE, assertThrows(PaymentException.class,
            () -> service.create(command("ORDER-1"), "client", "other", context("expired"))).errorCode());
        assertEquals(1, calls.get());
    }

    @Test void differentClientsCanUseSameKeyAndReference() {
        var service = service(c -> success());
        var a = service.create(command("ORDER-1"), "client-a", "key", context("a"));
        var b = service.create(command("ORDER-1"), "client-b", "key", context("b"));
        assertNotEquals(a.paymentId(), b.paymentId());
        assertEquals(a, service.create(command("ORDER-1"), "client-a", "key", context("a-replay")));
        assertEquals(2, calls.get());
    }

    @Test void retentionMustBePositive() {
        for (Duration retention : List.of(Duration.ZERO, Duration.ofSeconds(-1))) {
            assertThrows(IllegalArgumentException.class, () -> new PaymentService(List.of(), clock, retention, (c, l) -> c.code()));
        }
    }

    @Test void unsupportedProviderNeverCallsProvider() {
        var service = service(c -> success());
        var command = new CreatePaymentCommand("UNKNOWN", "ORDER-1", new Money("100000", "VND"), null);
        assertEquals(PaymentErrorCode.UNSUPPORTED_PROVIDER, assertThrows(PaymentException.class,
            () -> service.create(command, "client", "key", context("first"))).errorCode());
        assertEquals(0, calls.get());
    }
}
