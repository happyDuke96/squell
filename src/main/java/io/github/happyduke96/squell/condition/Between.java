package io.github.happyduke96.squell.condition;

import java.util.Arrays;
import java.util.List;

public final class Between<T> implements Condition {

    private final Field<T> field;
    private final T low;
    private final T high;

    Between(Field<T> field, T low, T high) {
        this.field = field;
        this.low = low;
        this.high = high;
    }

    @Override
    public String sql() {
        return field.name() + " BETWEEN ? AND ?";
    }

    @Override
    public List<Object> values() {
        return Arrays.asList(field.toSqlValue(low), field.toSqlValue(high));
    }
}
