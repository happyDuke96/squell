package io.github.happyduke96.squell.exception;

import java.sql.SQLException;

/// Thrown by `UpdateStep#execute` when a `whereVersion` guard matched no row — someone else
/// updated it first, instead of the write silently doing nothing.
public final class OptimisticLockException extends SQLException {

    public OptimisticLockException(String tableName) {
        super("[" + tableName + "] update matched no row at the expected version — it was updated concurrently.");
    }
}
