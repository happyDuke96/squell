package io.github.happyduke96.squell.connection;

import org.intellij.lang.annotations.MagicConstant;

import java.sql.Connection;

/// JDBC's four standard transaction isolation levels — pass one to `transaction(...)` to
/// override the database's default for that transaction.
public enum IsolationLevel {
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int jdbcLevel;

    IsolationLevel(int jdbcLevel) {
        this.jdbcLevel = jdbcLevel;
    }

    @MagicConstant(intValues = {
            Connection.TRANSACTION_READ_UNCOMMITTED,
            Connection.TRANSACTION_READ_COMMITTED,
            Connection.TRANSACTION_REPEATABLE_READ,
            Connection.TRANSACTION_SERIALIZABLE
    })
    public int jdbcLevel() {
        return jdbcLevel;
    }
}
