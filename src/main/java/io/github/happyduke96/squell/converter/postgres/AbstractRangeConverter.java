package io.github.happyduke96.squell.converter.postgres;

import io.github.happyduke96.squell.converter.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

/// Base for a `Converter` to a Postgres range column (`int4range`, ...); subclass and
/// implement `pgType`/`serializeBound`/`deserializeBound`. See `IntRangeConverter`.
public abstract class AbstractRangeConverter<T> implements Converter<Range<T>, PGobject> {

    @Override
    public final PGobject toSql(Range<T> value) {
        if (value == null) {
            return null;
        }
        try {
            PGobject sqlValue = new PGobject();
            sqlValue.setType(pgType());
            String lower = value.lower() == null ? "" : serializeBound(value.lower());
            String upper = value.upper() == null ? "" : serializeBound(value.upper());
            sqlValue.setValue("[" + lower + "," + upper + ")");
            return sqlValue;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Invalid range: " + value, e);
        }
    }

    @Override
    public final Range<T> fromSql(PGobject sqlValue) {
        if (sqlValue == null || sqlValue.getValue() == null) {
            return null;
        }
        String literal = sqlValue.getValue();
        int comma = literal.indexOf(',');
        String lowerText = literal.substring(1, comma);
        String upperText = literal.substring(comma + 1, literal.length() - 1);
        T lower = lowerText.isEmpty() ? null : deserializeBound(lowerText);
        T upper = upperText.isEmpty() ? null : deserializeBound(upperText);
        return new Range<>(lower, upper);
    }

    protected abstract String pgType();

    protected abstract String serializeBound(T bound);

    protected abstract T deserializeBound(String literal);
}
