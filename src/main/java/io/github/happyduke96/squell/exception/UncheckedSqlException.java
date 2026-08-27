package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class UncheckedSqlException extends RuntimeException {

    public UncheckedSqlException(SQLException cause) {
        super(cause.getMessage(), DataAccessException.translate(cause));
    }
}
