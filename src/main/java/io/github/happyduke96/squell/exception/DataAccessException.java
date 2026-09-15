package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

/// A `SQLException` translated to a specific subtype by SQLSTATE (`translate`), with a `retryable`
/// flag — e.g. `DuplicateKeyException` for a unique-constraint violation.
public class DataAccessException extends SQLException {

    private final boolean retryable;

    DataAccessException(String message, SQLException cause) {
        this(message, cause, false);
    }

    DataAccessException(String message, SQLException cause, boolean retryable) {
        super(message, cause.getSQLState(), cause.getErrorCode(), cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }

    /// MySQL's `ER_LOCK_NOWAIT` — a row already locked by another transaction, thrown by
    /// `forUpdateNoWait()`.
    private static final int MYSQL_LOCK_NOWAIT = 3572;

    /// MySQL's `ER_LOCK_DEADLOCK` — sharing SQLSTATE `40001` with plain lock-wait-timeout, so it
    /// needs the vendor error code to tell them apart.
    private static final int MYSQL_DEADLOCK = 1213;

    public static DataAccessException translate(SQLException cause) {
        String state = cause.getSQLState();
        if (state == null) {
            return new DataAccessException(cause.getMessage(), cause);
        }
        return switch (state) {
            case "23505" -> new DuplicateKeyException(cause);
            case "23503", "23506" -> new ForeignKeyViolationException(cause);
            case "23502" -> new NotNullViolationException(cause);
            case "23513", "23514" -> new CheckViolationException(cause);
            case "40P01" -> new PostgresDeadlockException(cause);
            case "55P03" -> new PostgresLockNotAvailableException(cause);
            case "HY000" -> cause.getErrorCode() == MYSQL_LOCK_NOWAIT
                    ? new MySqlLockNotAvailableException(cause)
                    : new DataAccessException(cause.getMessage(), cause);
            case "40001" -> cause.getErrorCode() == MYSQL_DEADLOCK
                    ? new MySqlDeadlockException(cause)
                    : new TransientDataAccessException(cause);
            case "HYT00" -> new TransientDataAccessException(cause);
            default -> new DataAccessException(cause.getMessage(), cause);
        };
    }
}
