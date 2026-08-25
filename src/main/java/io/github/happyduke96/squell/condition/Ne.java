package io.github.happyduke96.squell.condition;

import java.util.Collections;
import java.util.List;

public final class Ne<T> implements Condition {

    private final Field<T> field;
    private final T value;

    Ne(Field<T> field, T value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public String sql() {
        return field.name() + " <> ?";
    }

    @Override
    public List<Object> values() {
        return Collections.singletonList(field.toSqlValue(value));
    }
}
