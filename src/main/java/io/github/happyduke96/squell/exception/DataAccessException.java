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
            case "40001", "HYT00" -> new TransientDataAccessException(cause);
            default -> new DataAccessException(cause.getMessage(), cause);
        };
    }
}
