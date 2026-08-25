package io.github.happyduke96.squell.condition;

import java.util.List;

public final class AggregateLt<T> implements Condition {

    private final String expression;
    private final T value;

    AggregateLt(String expression, T value) {
        this.expression = expression;
        this.value = value;
    }

    @Override
    public String sql() {
        return expression + " < ?";
    }

    @Override
    public List<Object> values() {
        return List.of(value);
    }
}
