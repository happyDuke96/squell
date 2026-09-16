package io.github.happyduke96.squell.condition;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/// Case-insensitive `LIKE`, portable across dialects: lowers the column via `LOWER(...)` and
/// lowers `pattern` in Java (`Locale.ROOT`) before binding, rather than relying on a
/// dialect-specific operator like Postgres's `ILIKE` or a database's default collation.
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
