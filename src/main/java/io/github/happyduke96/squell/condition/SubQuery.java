package io.github.happyduke96.squell.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// An embedded `SELECT`, for `Field#in(SubQuery)`/`notIn(SubQuery)` — from `SelectStep#subQuery`.
public final class SubQuery<T> {

    private final String sql;
    private final List<Object> values;

    public SubQuery(String sql, List<Object> values) {
        this.sql = sql;
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    public String sql() {
        return sql;
    }

    public List<Object> values() {
        return values;
    }
}
