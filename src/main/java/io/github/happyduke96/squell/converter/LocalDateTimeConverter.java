package io.github.happyduke96.squell.converter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/// Converts `LocalDateTime` for binding — a hint-less `getObject` returns `Timestamp` on Postgres,
/// `LocalDateTime` itself on MySQL; reads accept either.
public final class LocalDateTimeConverter implements Converter<LocalDateTime, Object> {

    @Override
    public Object toSql(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    @Override
    public LocalDateTime fromSql(Object sqlValue) {
        return switch (sqlValue) {
            case null -> null;
            case Timestamp timestamp -> timestamp.toLocalDateTime();
            case LocalDateTime localDateTime -> localDateTime;
            default -> throw new IllegalArgumentException(
                    "Expected Timestamp or LocalDateTime, got " + sqlValue.getClass() + ".");
        };
    }
}
