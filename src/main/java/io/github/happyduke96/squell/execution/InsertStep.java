package io.github.happyduke96.squell.execution;

public interface InsertStep<T> {

    FilledInsertStep<T> values(T entity);
}
