package com.manh.partnerbridge.payment.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-09-19T00:00:00Z"));
    public void advance(Duration duration) { now.updateAndGet(time -> time.plus(duration)); }
    public ZoneId getZone() { return ZoneOffset.UTC; }
    public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
    public Instant instant() { return now.get(); }
}
