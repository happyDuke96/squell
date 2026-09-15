package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class MySqlLockNotAvailableException extends DataAccessException {

    MySqlLockNotAvailableException(SQLException cause) {
        super("Row is locked by another transaction — safe to retry after a short delay.", cause, true);
    }
}
