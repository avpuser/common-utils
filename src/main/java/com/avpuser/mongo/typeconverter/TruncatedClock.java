package com.avpuser.mongo.typeconverter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class TruncatedClock extends Clock {

    private final Clock delegate;

    public TruncatedClock(Clock delegate) {
        this.delegate = delegate;
    }

    @Override
    public ZoneId getZone() {
        return delegate.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new TruncatedClock(delegate.withZone(zone));
    }

    @Override
    public Instant instant() {
        // 🔧 Округляем до миллисекунд
        return delegate.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}