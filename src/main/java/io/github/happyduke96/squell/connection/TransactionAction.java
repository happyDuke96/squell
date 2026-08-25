package io.github.happyduke96.squell.connection;

import java.sql.SQLException;

/// A unit of work run inside `transactionWithoutResult` — rolled back as a whole if it throws.
@FunctionalInterface
public interface TransactionAction<TX> {
    void run(TX tx) throws SQLException;
}
