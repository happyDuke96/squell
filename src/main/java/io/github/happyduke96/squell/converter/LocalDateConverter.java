package io.github.happyduke96.squell.converter;

import java.sql.Date;
import java.time.LocalDate;

/// Converts `LocalDate` <-> `java.sql.Date` for binding — a hint-less `getObject` returns `Date`,
/// not `LocalDate`, on both dialects.
public final class LocalDateConverter implements Converter<LocalDate, Date> {

    @Override
    public Date toSql(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    @Override
    public LocalDate fromSql(Date sqlValue) {
        return sqlValue == null ? null : sqlValue.toLocalDate();
    }
}
