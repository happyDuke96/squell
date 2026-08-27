package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Condition;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

public interface ReturningDeleteStep<T> extends DeleteStep<T> {

    @Override
    ReturningDeleteStep<T> where(Condition condition);

    @Override
    default ReturningDeleteStep<T> whereIf(boolean test, Supplier<Condition> condition) {
        return test ? where(condition.get()) : this;
    }

    List<T> deleteAndReturn() throws SQLException;
}
