package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Condition;

import java.sql.SQLException;
import java.util.function.Supplier;

public interface DeleteStep<T> {

    DeleteStep<T> where(Condition condition);

    default DeleteStep<T> whereIf(boolean test, Supplier<Condition> condition) {
        return test ? where(condition.get()) : this;
    }

    int execute() throws SQLException;
}
