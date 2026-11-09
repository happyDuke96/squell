package io.github.happyduke96.squell.connection;

import java.sql.SQLException;

/**
 * A unit of work run by {@code transaction}; the transaction rolls back as a whole if it throws.
 *
 * @param <TX> the transaction type
 * @param <R> the result type
 */
@FunctionalInterface
public interface TransactionWork<TX, R> {
    R run(TX tx) throws SQLException;
}
