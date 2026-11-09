package io.github.happyduke96.squell.condition;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Case-insensitive {@code LIKE} that is portable across dialects.
 *
 * <p>It lowers the column with {@code LOWER(...)} and the pattern in Java ({@code Locale.ROOT})
 * instead of relying on a dialect operator such as Postgres {@code ILIKE}.
 */
public final class ILike implements Condition {

    private final Field<?> field;
    private final String pattern;

    ILike(Field<?> field, String pattern) {
        this.field = field;
        this.pattern = pattern;
    }

    @Override
    public String sql() {
        return "LOWER(" + field.name() + ") LIKE ?";
    }

    @Override
    public List<Object> values() {
        return Collections.singletonList(pattern == null ? null : pattern.toLowerCase(Locale.ROOT));
    }
}
