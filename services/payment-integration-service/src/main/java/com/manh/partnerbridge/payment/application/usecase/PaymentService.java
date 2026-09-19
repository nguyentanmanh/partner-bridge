package com.manh.partnerbridge.payment.application.usecase;

import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.command.PaymentRequestContext;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.port.in.PaymentUseCase;
import com.manh.partnerbridge.payment.application.port.out.PaymentMessagesPort;
import com.manh.partnerbridge.payment.application.port.out.PaymentProviderPort;
import com.manh.partnerbridge.payment.application.query.PaymentFailure;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.domain.model.Payment;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PaymentService implements PaymentUseCase {
    private final Map<String, PaymentProviderPort> providers;
    private final Object reservationLock = new Object();
    private final Map<ScopedKey, Entry> idempotency = new HashMap<>();
    private final Set<ScopedKey> references = new HashSet<>();
    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration retention;
    private final PaymentMessagesPort messages;

    public PaymentService(List<PaymentProviderPort> providers, Clock clock, Duration retention, PaymentMessagesPort messages) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("partnerbridge.idempotency.retention must be greater than zero");
        }
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(PaymentProviderPort::providerCode, p -> p));
        this.clock = clock;
        this.retention = retention;
        this.messages = messages;
    }

    @Override
    public Payment create(CreatePaymentCommand command, String clientId, String key, PaymentRequestContext context) {
        if (clientId == null || clientId.isBlank()) throw new IllegalArgumentException("client scope must not be blank");
        PaymentProviderPort provider = providers.get(command.providerCode());
        Entry entry;
        boolean owner;
        synchronized (reservationLock) {
            Instant now = clock.instant();
            idempotency.values().removeIf(e -> e.state != ProcessingState.IN_FLIGHT && !e.expiresAt.isAfter(now));
            ScopedKey scopedKey = new ScopedKey(clientId, key);
            entry = idempotency.get(scopedKey);
            owner = entry == null;
            if (!owner) {
                if (!entry.command.equals(command)) throw new PaymentException(PaymentErrorCode.IDEMPOTENCY_CONFLICT);
            } else {
                ScopedKey reference = new ScopedKey(clientId, command.merchantReference());
                if (references.contains(reference)) throw new PaymentException(PaymentErrorCode.DUPLICATE_REFERENCE);
                if (provider == null) throw new PaymentException(PaymentErrorCode.UNSUPPORTED_PROVIDER);
                entry = new Entry(command);
                idempotency.put(scopedKey, entry);
                references.add(reference);
            }
        }
        if (owner) process(entry, provider, command, context);
        // Waiting is per reservation. Independent creates and GET do not acquire this future.
        Outcome result = entry.result.join();
        if (result.failure != null) throw new PaymentException(result.failure);
        return result.payment;
    }

    private void process(Entry entry, PaymentProviderPort provider, CreatePaymentCommand command, PaymentRequestContext context) {
        Outcome outcome;
        ProcessingState state;
        Instant started = clock.instant();
        try {
            PaymentProviderPort.ProviderResult result = provider.create(command, context.requestId());
            Payment payment = new Payment(UUID.randomUUID().toString(), command.merchantReference(), command.providerCode(),
                result.transactionId(), command.amount(), result.status(), started, clock.instant());
            payments.put(payment.paymentId(), payment);
            outcome = new Outcome(payment, null);
            state = ProcessingState.COMPLETED;
        } catch (RuntimeException exception) {
            PaymentErrorCode code = exception instanceof PaymentException paymentException
                ? paymentException.errorCode() : PaymentErrorCode.INTERNAL_ERROR;
            String message;
            try {
                message = messages.resolve(code, context.locale());
            } catch (RuntimeException messageFailure) {
                message = code.code(); // Safe fallback; always finish the reservation.
            }
            outcome = new Outcome(null, new PaymentFailure(code, message, context.traceId(), context.requestId(), clock.instant()));
            state = code == PaymentErrorCode.PROVIDER_REJECTED || code == PaymentErrorCode.PROVIDER_AUTH
                ? ProcessingState.COMPLETED : ProcessingState.UNKNOWN;
        }
        synchronized (reservationLock) {
            // TTL starts only after provider processing and error presentation have finished.
            entry.expiresAt = clock.instant().plus(retention);
            entry.state = state;
            entry.result.complete(outcome);
        }
    }

    @Override
    public Payment get(String paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null) throw new PaymentException(PaymentErrorCode.NOT_FOUND);
        return payment;
    }

    /** Internal state is deliberately independent of canonical PaymentStatus. */
    private enum ProcessingState { IN_FLIGHT, COMPLETED, UNKNOWN }
    private record ScopedKey(String clientId, String value) {}
    private record Outcome(Payment payment, PaymentFailure failure) {}

    private static final class Entry {
        private final CreatePaymentCommand command;
        private final CompletableFuture<Outcome> result = new CompletableFuture<>();
        // These fields are accessed only while holding reservationLock.
        private ProcessingState state = ProcessingState.IN_FLIGHT;
        private Instant expiresAt;

        private Entry(CreatePaymentCommand command) { this.command = command; }
    }
}
