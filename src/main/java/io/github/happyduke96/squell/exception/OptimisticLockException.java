package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

/**
 * Thrown by {@code UpdateStep#execute} when a {@code whereVersion} guard matches no row, meaning
 * another transaction updated it first.
 *
 * @since 0.3.0
 */
public final class OptimisticLockException extends SQLException {

    public OptimisticLockException(String tableName) {
        super("[" + tableName + "] update matched no row at the expected version — it was updated concurrently.");
    }
}
