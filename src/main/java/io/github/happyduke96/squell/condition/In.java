package io.github.happyduke96.squell.condition;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class In<T> implements Condition {

    private final Field<T> field;
    private final Collection<T> values;

    In(Field<T> field, Collection<T> values) {
        this.field = field;
        this.values = values;
    }

    @Override
    public String sql() {
        if (values.isEmpty()) {
            return "1 = 0";
        }
        return field.name() + " IN (" + values.stream().map(v -> "?").collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public List<Object> values() {
        return values.stream().map(field::toSqlValue).toList();
    }
}
