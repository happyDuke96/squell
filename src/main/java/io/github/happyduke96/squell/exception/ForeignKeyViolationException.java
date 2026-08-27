package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class ForeignKeyViolationException extends DataAccessException {

    ForeignKeyViolationException(SQLException cause) {
        super("Write violates a foreign key constraint.", cause);
    }
}
