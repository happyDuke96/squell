package io.github.happyduke96.squell.condition;

import java.util.List;

public final class IsNotNull implements Condition {

    private final Field<?> field;

    IsNotNull(Field<?> field) {
        this.field = field;
    }

    @Override
    public String sql() {
        return field.name() + " IS NOT NULL";
    }

    @Override
    public List<Object> values() {
        return List.of();
    }
}
