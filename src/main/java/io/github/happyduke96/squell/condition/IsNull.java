package io.github.happyduke96.squell.condition;

import java.util.List;

public final class IsNull implements Condition {

    private final Field<?> field;

    IsNull(Field<?> field) {
        this.field = field;
    }

    @Override
    public String sql() {
        return field.name() + " IS NULL";
    }

    @Override
    public List<Object> values() {
        return List.of();
    }
}
