package io.github.happyduke96.squell.condition;

import java.util.Collections;
import java.util.List;

public final class AggregateEq<T> implements Condition {

    private final String expression;
    private final T value;

    AggregateEq(String expression, T value) {
        this.expression = expression;
        this.value = value;
    }

    @Override
    public String sql() {
        return expression + " = ?";
    }

    @Override
    public List<Object> values() {
        return Collections.singletonList(value);
    }
}
