package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

public interface ReturningUpdateStep<T> extends UpdateStep<T> {

    @Override
    <V> ReturningUpdateStep<T> set(Field<V> field, V value);

    @Override
    ReturningUpdateStep<T> where(Condition condition);

    @Override
    default ReturningUpdateStep<T> whereIf(boolean test, Supplier<Condition> condition) {
        return test ? where(condition.get()) : this;
    }

    List<T> updateAndReturn() throws SQLException;
}
