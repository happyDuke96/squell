package io.github.happyduke96.squell.condition;

import java.util.List;

public final class NotLike implements Condition {

    private final Field<?> field;
    private final String pattern;

    NotLike(Field<?> field, String pattern) {
        this.field = field;
        this.pattern = pattern;
    }

    @Override
    public String sql() {
        return field.name() + " NOT LIKE ?";
    }

    @Override
    public List<Object> values() {
        return List.of(pattern);
    }
}
