package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class DuplicateKeyException extends DataAccessException {

    DuplicateKeyException(SQLException cause) {
        super("Duplicate key violates a unique constraint.", cause);
    }
}
