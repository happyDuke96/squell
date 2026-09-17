package io.github.happyduke96.squell.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class NotIn<T> implements Condition {

    private final Field<T> field;
    private final Collection<T> values;

    NotIn(Field<T> field, Collection<T> values) {
        this.field = field;
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Override
    public String sql() {
        if (values.isEmpty()) {
            return "1 = 1";
        }
        StringBuilder placeholders = new StringBuilder(values.size() * 3 - 2);
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                placeholders.append(", ");
            }
            placeholders.append('?');
        }
        return field.name() + " NOT IN (" + placeholders + ")";
    }

    @Override
    public List<Object> values() {
        List<Object> converted = new ArrayList<>(values.size());
        for (T value : values) {
            converted.add(field.toSqlValue(value));
        }
        return converted;
    }
}
