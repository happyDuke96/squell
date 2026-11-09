package io.github.happyduke96.squell.converter;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Converts {@code LocalDate} to and from {@code java.sql.Date} for binding.
 *
 * <p>A hint-less {@code getObject} returns {@code Date}, not {@code LocalDate}, on both dialects.
 */
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
