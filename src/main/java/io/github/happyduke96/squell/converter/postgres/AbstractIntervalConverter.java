package io.github.happyduke96.squell.converter.postgres;

import io.github.happyduke96.squell.converter.Converter;
import org.postgresql.util.PGInterval;

/**
 * Base class for a converter to a Postgres {@code interval} column.
 *
 * <p>Subclasses implement {@code serialize} and {@code deserialize} against {@code PGInterval}; see
 * {@code DurationIntervalConverter}.
 *
 * @param <T> the Java type
 */
public abstract class AbstractIntervalConverter<T> implements Converter<T, PGInterval> {

    @Override
    public final PGInterval toSql(T value) {
        return value == null ? null : serialize(value);
    }

    @Override
    public final T fromSql(PGInterval sqlValue) {
        return sqlValue == null ? null : deserialize(sqlValue);
    }

    protected abstract PGInterval serialize(T value);

    protected abstract T deserialize(PGInterval interval);
}
