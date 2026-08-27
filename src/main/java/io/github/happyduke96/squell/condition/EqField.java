package io.github.happyduke96.squell.condition;

import java.util.List;

public final class EqField<T> implements Condition {

    private final Field<T> left;
    private final Field<T> right;

    EqField(Field<T> left, Field<T> right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String sql() {
        return left.name() + " = " + right.name();
    }

    @Override
    public List<Object> values() {
        return List.of();
    }
}
