package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

import java.sql.SQLException;
import java.util.function.Supplier;

public interface UpdateStep<T> {

    <V> UpdateStep<T> set(Field<V> field, V value);

    <V> UpdateStep<T> set(Field<V> field, Field<V> other);

    <N extends Number> UpdateStep<T> increment(Field<N> field, N delta);

    UpdateStep<T> where(Condition condition);

    default UpdateStep<T> whereIf(boolean test, Supplier<Condition> condition) {
        return test ? where(condition.get()) : this;
    }

    int execute() throws SQLException;
}
