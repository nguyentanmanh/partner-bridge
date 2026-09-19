package com.manh.partnerbridge.payment.application.query;

import com.manh.partnerbridge.payment.domain.exception.PaymentErrorCode;
import java.time.Instant;

/** Immutable presentation values from the first attempt, retained for exact error replay. */
public record PaymentFailure(PaymentErrorCode code, String message, String traceId,
                             String requestId, Instant timestamp) {}
