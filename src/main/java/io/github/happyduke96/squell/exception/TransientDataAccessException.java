package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

public final class TransientDataAccessException extends DataAccessException {

    TransientDataAccessException(SQLException cause) {
        super("Transient failure — safe to retry.", cause, true);
    }
}
