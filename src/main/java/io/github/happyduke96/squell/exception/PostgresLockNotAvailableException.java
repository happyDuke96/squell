package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class PostgresLockNotAvailableException extends DataAccessException {

    PostgresLockNotAvailableException(SQLException cause) {
        super("Row is locked by another transaction — safe to retry after a short delay.", cause, true);
    }
}
