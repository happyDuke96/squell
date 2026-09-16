package io.github.happyduke96.squell.converter;

import java.sql.Time;
import java.time.LocalTime;

/// Converts `LocalTime` <-> `java.sql.Time` for binding — a hint-less `getObject` returns `Time`,
/// not `LocalTime`. `Time` also has no sub-second precision, so nanos are lost either way.
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
