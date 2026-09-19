package com.manh.partnerbridge.payment.application.command;

import java.util.Locale;

public record PaymentRequestContext(String requestId, String traceId, Locale locale) {}
