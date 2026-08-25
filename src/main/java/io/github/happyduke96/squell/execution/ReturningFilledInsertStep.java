package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Field;

import java.sql.SQLException;

public interface ReturningFilledInsertStep<T> extends FilledInsertStep<T> {

    T executeAndReturn() throws SQLException;

    OnConflictStep<T> onConflict(Field<?>... conflictTargets);
}
