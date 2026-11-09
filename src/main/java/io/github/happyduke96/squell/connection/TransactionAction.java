package io.github.happyduke96.squell.connection;

import java.sql.SQLException;

/**
 * A unit of work run by {@code transactionWithoutResult}; the transaction rolls back as a whole if
 * it throws.
 *
 * @param <TX> the transaction type
 */
@FunctionalInterface
public interface TransactionAction<TX> {
    void run(TX tx) throws SQLException;
}
