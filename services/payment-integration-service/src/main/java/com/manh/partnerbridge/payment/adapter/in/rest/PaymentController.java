package com.manh.partnerbridge.payment.adapter.in.rest;

import com.manh.partnerbridge.payment.application.command.CreatePaymentCommand;
import com.manh.partnerbridge.payment.application.command.PaymentRequestContext;
import com.manh.partnerbridge.payment.application.exception.PaymentException;
import com.manh.partnerbridge.payment.application.port.in.PaymentUseCase;
import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import com.manh.partnerbridge.payment.infrastructure.observability.TraceIdProvider;
import com.manh.partnerbridge.payment.domain.model.Money;
import com.manh.partnerbridge.payment.domain.model.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentUseCase useCase;
    private final String clientId;
    private final boolean trustedIdentityRequired;
    private final TraceIdProvider traceIds;

    public PaymentController(PaymentUseCase useCase, @Value("${partnerbridge.client.default-id}") String clientId,
                             @Value("${partnerbridge.client.trusted-identity-required:false}") boolean trustedIdentityRequired,
                             TraceIdProvider traceIds) {
        if (clientId.isBlank()) throw new IllegalArgumentException("partnerbridge.client.default-id must not be blank");
        this.useCase = useCase;
        this.clientId = clientId;
        this.trustedIdentityRequired = trustedIdentityRequired;
        this.traceIds = traceIds;
    }

    @PostMapping
    public ResponseEntity<Payment> create(
        @RequestHeader("Request-ID") @Pattern(regexp = "^[!-~]{1,128}$") String requestId,
        @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[!-~]{1,128}$") String key,
        @RequestHeader(value = "X-Authenticated-Client-Id", required = false) String authenticatedClientId,
        @Valid @RequestBody CreatePaymentRequest request) {
        String scope = clientScope(authenticatedClientId);
        Payment payment = useCase.create(new CreatePaymentCommand(request.providerCode(),
                request.merchantReference(),
                new Money(request.amount().value(), request.amount().currency()), request.description()), scope, key,
            new PaymentRequestContext(requestId, traceIds.currentTraceId(), LocaleContextHolder.getLocale()));
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    private String clientScope(String authenticatedClientId) {
        if (trustedIdentityRequired) {
            if (authenticatedClientId == null || !authenticatedClientId.matches("^[!-~]{1,128}$")) {
                throw new PaymentException(PaymentErrorCode.INVALID_REQUEST);
            }
            return authenticatedClientId;
        }
        return clientId;
    }

    @GetMapping("/{paymentId}")
    public Payment get(@RequestHeader("Request-ID") @Pattern(regexp = "^[!-~]{1,128}$") String requestId,
                       @RequestHeader(value = "X-Authenticated-Client-Id", required = false) String authenticatedClientId,
                       @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String paymentId) {
        return useCase.get(paymentId, clientScope(authenticatedClientId));
    }
}
