package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Field;

import java.sql.SQLException;
import java.util.Optional;

public interface OnConflictStep<T> {

    T doUpdate() throws SQLException;

    T doUpdate(Field<?>... columnsToUpdate) throws SQLException;

    Optional<T> doNothing() throws SQLException;
}
