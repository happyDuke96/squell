package io.github.happyduke96.squell.execution;

import java.sql.SQLException;

public interface FilledInsertStep<T> {

    void execute() throws SQLException;
}
