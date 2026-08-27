package io.github.happyduke96.squell.converter.postgres;

import io.github.happyduke96.squell.converter.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

/// Base for a `Converter` to a Postgres `jsonb` column; subclass and implement
/// `serialize`/`deserialize` to/from the JSON text. See `JsonbConverter`.
public abstract class AbstractJsonbConverter<T> implements Converter<T, PGobject> {

    @Override
    public final PGobject toSql(T value) {
        if (value == null) {
            return null;
        }
        try {
            PGobject sqlValue = new PGobject();
            sqlValue.setType("jsonb");
            sqlValue.setValue(serialize(value));
            return sqlValue;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Invalid JSON: [" + value + "]", e);
        }
    }

    @Override
    public final T fromSql(PGobject sqlValue) {
        return (sqlValue == null || sqlValue.getValue() == null) ? null : deserialize(sqlValue.getValue());
    }

    protected abstract String serialize(T value);

    protected abstract T deserialize(String json);
}
