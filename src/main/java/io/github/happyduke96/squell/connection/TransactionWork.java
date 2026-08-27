package io.github.happyduke96.squell.connection;

import java.sql.SQLException;

/// A unit of work run inside `transaction`, returning `R` — rolled back as a whole if it throws.
@FunctionalInterface
public interface TransactionWork<TX, R> {
    R run(TX tx) throws SQLException;
}
