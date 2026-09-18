package com.manh.partnerbridge.payment.infrastructure.observability;

import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

@Component
public class TraceIdProvider {
    private final Tracer tracer;

    public TraceIdProvider(Tracer tracer) {
        this.tracer = tracer;
    }

    public String currentTraceId() {
        var span = tracer.currentSpan();
        return span == null ? "" : span.context().traceId();
    }
}
