package io.github.happyduke96.squell.condition;

import java.util.List;
import java.util.Locale;

/// The negation of `ILike` — see it for why `LOWER(...)` instead of a dialect-specific operator.
public final class NotILike implements Condition {

    private final Field<?> field;
    private final String pattern;

    NotILike(Field<?> field, String pattern) {
        this.field = field;
        this.pattern = pattern;
    }

    @Override
    public String sql() {
        return "LOWER(" + field.name() + ") NOT LIKE ?";
    }

    @Override
    public List<Object> values() {
        return List.of(pattern.toLowerCase(Locale.ROOT));
    }
}
