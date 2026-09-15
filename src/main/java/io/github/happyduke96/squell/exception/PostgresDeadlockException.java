package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class PostgresDeadlockException extends DataAccessException {

    PostgresDeadlockException(SQLException cause) {
        super("Deadlock detected — safe to retry the whole transaction.", cause, true);
    }
}
