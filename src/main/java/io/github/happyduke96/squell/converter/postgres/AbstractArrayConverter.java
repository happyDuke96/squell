package io.github.happyduke96.squell.converter.postgres;

import io.github.happyduke96.squell.converter.Converter;
import org.postgresql.util.PGobject;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/// Base for a `Converter` to a Postgres array column (`text[]`, `int4[]`, ...); subclass and
/// implement `pgType`/`serializeElement`/`deserializeElement` for the element type.
public abstract class AbstractArrayConverter<T> implements Converter<List<T>, Object> {

    private static final Pattern NEEDS_QUOTING = Pattern.compile("[{},\"\\\\\\s]");

    @Override
    public final Object toSql(List<T> value) {
        if (value == null) {
            return null;
        }
        try {
            PGobject sqlValue = new PGobject();
            sqlValue.setType(pgType());
            sqlValue.setValue(formatElements(value));
            return sqlValue;
        } catch (SQLException e) {
            throw new IllegalArgumentException("Invalid array: " + value, e);
        }
    }

    @Override
    public final List<T> fromSql(Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        try {
            if (sqlValue instanceof Array array) {
                return fromElements((Object[]) array.getArray());
            }
            if (sqlValue instanceof PGobject pgObject) {
                return pgObject.getValue() == null ? null : fromLiteral(pgObject.getValue());
            }
            throw new IllegalArgumentException("Unsupported array wire value: " + sqlValue.getClass());
        } catch (SQLException e) {
            throw new IllegalArgumentException("Invalid array: " + sqlValue, e);
        }
    }

    private List<T> fromElements(Object[] rawElements) {
        List<T> elements = new ArrayList<>();
        for (Object rawElement : rawElements) {
            elements.add(rawElement == null ? null : deserializeElement(rawElement.toString()));
        }
        return elements;
    }

    private List<T> fromLiteral(String arrayLiteral) {
        List<T> elements = new ArrayList<>();
        for (String literal : parseElements(arrayLiteral)) {
            elements.add(literal == null ? null : deserializeElement(literal));
        }
        return elements;
    }

    protected abstract String pgType();

    protected abstract String serializeElement(T element);

    protected abstract T deserializeElement(String literal);

    private String formatElements(List<T> elements) {
        return elements.stream()
                .map(e -> e == null ? "NULL" : quote(serializeElement(e)))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String quote(String literal) {
        if (!NEEDS_QUOTING.matcher(literal).find() && !literal.isEmpty() && !literal.equalsIgnoreCase("NULL")) {
            return literal;
        }
        return "\"" + literal.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static List<String> parseElements(String arrayLiteral) {
        String inner = arrayLiteral.substring(1, arrayLiteral.length() - 1);
        List<String> elements = new ArrayList<>();
        if (inner.isEmpty()) {
            return elements;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean quoted = false;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (inQuotes) {
                if (c == '\\' && i + 1 < inner.length()) {
                    current.append(inner.charAt(++i));
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
                quoted = true;
            } else if (c == ',') {
                elements.add(toElement(current, quoted));
                current.setLength(0);
                quoted = false;
            } else {
                current.append(c);
            }
        }
        elements.add(toElement(current, quoted));
        return elements;
    }

    private static String toElement(StringBuilder current, boolean quoted) {
        String text = current.toString();
        return !quoted && text.equals("NULL") ? null : text;
    }
}
