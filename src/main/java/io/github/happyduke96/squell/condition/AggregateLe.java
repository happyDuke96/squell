package io.github.happyduke96.squell.condition;

import java.util.Collections;
import java.util.List;

public final class AggregateLe<T> implements Condition {

    private final String expression;
    private final T value;

    AggregateLe(String expression, T value) {
        this.expression = expression;
        this.value = value;
    }

    @Override
    public String sql() {
        return expression + " <= ?";
    }

    @Override
    public List<Object> values() {
        return Collections.singletonList(value);
    }
}
