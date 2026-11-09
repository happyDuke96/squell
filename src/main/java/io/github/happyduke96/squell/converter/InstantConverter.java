package io.github.happyduke96.squell.converter;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Converts {@code Instant} to and from {@code Timestamp} for binding.
 *
 * <p>pgjdbc cannot infer a SQL type for a bare {@code Instant}. Reads accept {@code Timestamp}
 * (Postgres) or {@code LocalDateTime} (MySQL).
 */
public final class InstantConverter implements Converter<Instant, Object> {

    @Override
    public Object toSql(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    @Override
    public Instant fromSql(Object sqlValue) {
        return switch (sqlValue) {
            case null -> null;
            case Timestamp timestamp -> timestamp.toInstant();
            case LocalDateTime localDateTime -> localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            default -> throw new IllegalArgumentException(
                    "Expected Timestamp or LocalDateTime, got " + sqlValue.getClass() + ".");
        };
    }
}
