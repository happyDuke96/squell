package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class MySqlDeadlockException extends DataAccessException {

    MySqlDeadlockException(SQLException cause) {
        super("Deadlock detected — safe to retry the whole transaction.", cause, true);
    }
}
