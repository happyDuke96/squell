package io.github.happyduke96.squell.converter.postgres;

import org.postgresql.util.PGInterval;

import java.time.Duration;

public final class DurationIntervalConverter extends AbstractIntervalConverter<Duration> {

    @Override
    protected PGInterval serialize(Duration value) {
        long totalSeconds = value.getSeconds();
        int days = (int) (totalSeconds / 86400);
        long remaining = totalSeconds % 86400;
        int hours = (int) (remaining / 3600);
        remaining %= 3600;
        int minutes = (int) (remaining / 60);
        double seconds = (remaining % 60) + value.getNano() / 1_000_000_000.0;
        return new PGInterval(0, 0, days, hours, minutes, seconds);
    }

    @Override
    protected Duration deserialize(PGInterval interval) {
        if (interval.getYears() != 0 || interval.getMonths() != 0) {
            throw new IllegalArgumentException(
                    "Interval [" + interval.getValue() + "] has years/months; Duration cannot represent that exactly.");
        }
        return Duration.ofDays(interval.getDays())
                .plusHours(interval.getHours())
                .plusMinutes(interval.getMinutes())
                .plusSeconds(interval.getWholeSeconds())
                .plusNanos(interval.getMicroSeconds() * 1000L);
    }
}
