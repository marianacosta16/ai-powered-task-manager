package com.matriz.testsupport;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock the tests move, to control "today" (R29). */
public class MutableClock extends Clock {

    private volatile Instant instant = Instant.parse("2026-06-01T10:00:00Z");

    public void set(String isoInstant) {
        this.instant = Instant.parse(isoInstant);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
