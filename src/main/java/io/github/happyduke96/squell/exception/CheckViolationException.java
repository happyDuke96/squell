package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class CheckViolationException extends DataAccessException {

    CheckViolationException(SQLException cause) {
        super("Write violates a CHECK constraint.", cause);
    }
}
