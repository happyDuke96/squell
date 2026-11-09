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

    /**
     * Guards the update with {@code WHERE field = expectedVersion} and increments {@code field}.
     *
     * <p>If no row matches, {@code execute()} throws {@code OptimisticLockException} instead of
     * returning {@code 0}.
     *
     * @param field the {@code @Version} field
     * @param expectedVersion the version read earlier
     * @param <V> the version type
     * @return this step with the guard
     * @since 0.3.0
     */
    <V> UpdateStep<T> whereVersion(Field<V> field, V expectedVersion);

    int execute() throws SQLException;
}
