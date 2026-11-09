package io.github.happyduke96.squell.converter;

import java.sql.Time;
import java.time.LocalTime;

/**
 * Converts {@code LocalTime} to and from {@code java.sql.Time} for binding.
 *
 * <p>{@code Time} has no sub-second precision, so nanoseconds are lost.
 */
public final class LocalTimeConverter implements Converter<LocalTime, Time> {

    @Override
    public Time toSql(LocalTime value) {
        return value == null ? null : Time.valueOf(value);
    }

    @Override
    public LocalTime fromSql(Time sqlValue) {
        return sqlValue == null ? null : sqlValue.toLocalTime();
    }
}
