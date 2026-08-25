package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class NotNullViolationException extends DataAccessException {

    NotNullViolationException(SQLException cause) {
        super("Write violates a NOT NULL constraint.", cause);
    }
}
