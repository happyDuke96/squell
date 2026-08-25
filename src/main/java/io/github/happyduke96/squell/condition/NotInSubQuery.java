package io.github.happyduke96.squell.condition;

import java.util.List;

public final class NotInSubQuery<T> implements Condition {

    private final Field<T> field;
    private final SubQuery<T> subQuery;

    NotInSubQuery(Field<T> field, SubQuery<T> subQuery) {
        this.field = field;
        this.subQuery = subQuery;
    }

    @Override
    public String sql() {
        return field.name() + " NOT IN (" + subQuery.sql() + ")";
    }

    @Override
    public List<Object> values() {
        return subQuery.values();
    }
}
