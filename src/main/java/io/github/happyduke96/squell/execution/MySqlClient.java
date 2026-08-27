package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.sql.Table;

import io.github.happyduke96.squell.connection.TransactionAction;
import io.github.happyduke96.squell.connection.TransactionWork;
import io.github.happyduke96.squell.execution.impl.DefaultClient;
import io.github.happyduke96.squell.internal.DialectVerifier;
import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/// Entry point for MySQL: `select`/`insert`/`update`/`delete`/`sql`, plus `validateSchema` and
/// `transaction`. Verifies at construction that `dataSource` is actually MySQL (or H2 for tests).
public final class MySqlClient {

    private final DefaultClient delegate;

    public MySqlClient(DataSource dataSource) throws SQLException {
        new DialectVerifier(dataSource, "MySqlClient", "MySQL", "H2").verify();
        this.delegate = new DefaultClient(dataSource);
    }

    public <T> SelectStep<T> select(Table<T> table) {
        return delegate.select(table);
    }

    public <T> InsertStep<T> insert(Table<T> table) {
        return delegate.insert(table);
    }

    public <T> UpdateStep<T> update(Table<T> table) {
        return delegate.update(table);
    }

    public <T> DeleteStep<T> delete(Table<T> table) {
        return delegate.delete(table);
    }

    public <T> int[] insertBatch(Table<T> table, List<T> entities) throws SQLException {
        return delegate.insertBatch(table, entities);
    }

    public <T> int[] updateBatch(Table<T> table, List<T> entities, Field<?> matchField) throws SQLException {
        return delegate.updateBatch(table, entities, matchField);
    }

    /// Raw SQL, bound positionally to its `?` placeholders — `sql` is not escaped or
    /// validated; never build it by concatenating untrusted input.
    public RawStep sql(@Language("SQL") String sql, Object... params) {
        return delegate.sql(sql, params);
    }

    public List<String> validateSchema(Table<?>... tables) throws SQLException {
        return delegate.validateSchema(tables);
    }

    public List<String> validateSchema(Collection<Table<?>> tables) throws SQLException {
        return delegate.validateSchema(tables);
    }

    public void validateSchemaOrThrow(Table<?>... tables) throws SQLException {
        delegate.validateSchemaOrThrow(tables);
    }

    public void validateSchemaOrThrow(Collection<Table<?>> tables) throws SQLException {
        delegate.validateSchemaOrThrow(tables);
    }

    public <R> R transaction(TransactionWork<Transaction, R> work) throws SQLException {
        return delegate.transaction(defaultTx -> work.run(new Transaction(defaultTx)));
    }

    public void transactionWithoutResult(TransactionAction<Transaction> action) throws SQLException {
        transaction(tx -> {
            action.run(tx);
            return null;
        });
    }

    public static final class Transaction {
        private final DefaultClient.Transaction delegate;

        private Transaction(DefaultClient.Transaction delegate) {
            this.delegate = delegate;
        }

        public <T> SelectStep<T> select(Table<T> table) {
            return delegate.select(table);
        }

        public <T> InsertStep<T> insert(Table<T> table) {
            return delegate.insert(table);
        }

        public <T> UpdateStep<T> update(Table<T> table) {
            return delegate.update(table);
        }

        public <T> DeleteStep<T> delete(Table<T> table) {
            return delegate.delete(table);
        }

        /// Raw SQL, bound positionally to its `?` placeholders — `sql` is not escaped or
        /// validated; never build it by concatenating untrusted input.
        public RawStep sql(@Language("SQL") String sql, Object... params) {
            return delegate.sql(sql, params);
        }
    }
}
