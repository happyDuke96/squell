package io.github.happyduke96.squell.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An embedded {@code SELECT} for {@code Field#in(SubQuery)} and {@code notIn(SubQuery)}.
 *
 * <p>Created by {@code SelectStep#subQuery}.
 *
 * @param <T> the type of the selected column
 */
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
