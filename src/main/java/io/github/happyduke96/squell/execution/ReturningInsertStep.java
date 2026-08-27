package io.github.happyduke96.squell.execution;

public interface ReturningInsertStep<T> extends InsertStep<T> {

    @Override
    ReturningFilledInsertStep<T> values(T entity);
}
