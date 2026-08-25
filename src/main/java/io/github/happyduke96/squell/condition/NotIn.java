package io.github.happyduke96.squell.condition;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class NotIn<T> implements Condition {

    private final Field<T> field;
    private final Collection<T> values;

    NotIn(Field<T> field, Collection<T> values) {
        this.field = field;
        this.values = values;
    }

    @Override
    public String sql() {
        if (values.isEmpty()) {
            return "1 = 1";
        }
        return field.name() + " NOT IN (" + values.stream().map(v -> "?").collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public List<Object> values() {
        return values.stream().map(field::toSqlValue).toList();
    }
}
