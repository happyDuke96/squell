package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.sql.AliasedTable;
import io.github.happyduke96.squell.sql.Table;

import io.github.happyduke96.squell.connection.IsolationLevel;
import io.github.happyduke96.squell.connection.TransactionAction;
import io.github.happyduke96.squell.connection.TransactionWork;
import io.github.happyduke96.squell.execution.impl.DefaultClient;
import io.github.happyduke96.squell.internal.DialectVerifier;
import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

/// Entry point for SQLite: `select`/`insert`/`update`/`delete`/`sql`, plus `validateSchema` and
/// `transaction`.
public final class SqliteClient {

    private final DefaultClient delegate;

    public SqliteClient(DataSource dataSource) throws SQLException {
        new DialectVerifier(dataSource, "SqliteClient", "SQLite", "H2").verify();
        this.delegate = new DefaultClient(dataSource);
    }

    public <T> SelectStep<T> select(Table<T> table) {
        return delegate.select(table);
    }

    public JoinStep select(AliasedTable<?> table) {
        return delegate.select(table);
    }

    public <T> ReturningInsertStep<T> insert(Table<T> table) {
        return delegate.insert(table);
    }

    public <T> ReturningUpdateStep<T> update(Table<T> table) {
        return delegate.update(table);
    }

    public <T> ReturningDeleteStep<T> delete(Table<T> table) {
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

    public <R> R transaction(IsolationLevel isolationLevel, TransactionWork<Transaction, R> work)
            throws SQLException {
        return delegate.transaction(isolationLevel, defaultTx -> work.run(new Transaction(defaultTx)));
    }

    public void transactionWithoutResult(TransactionAction<Transaction> action) throws SQLException {
        transaction(tx -> {
            action.run(tx);
            return null;
        });
    }

    public void transactionWithoutResult(IsolationLevel isolationLevel, TransactionAction<Transaction> action)
            throws SQLException {
        transaction(isolationLevel, tx -> {
            action.run(tx);
            return null;
        });
    }

    /// Retries `work` in a fresh `transaction(...)` while it fails with a retryable
    /// `DataAccessException` (e.g. `SQLITE_BUSY`), up to `maxAttempts` attempts, waiting
    /// `delayBetweenAttempts` between them.
    public <R> R transactionWithRetry(int maxAttempts, Duration delayBetweenAttempts,
            TransactionWork<Transaction, R> work) throws SQLException {
        return delegate.transactionWithRetry(maxAttempts, delayBetweenAttempts,
                defaultTx -> work.run(new Transaction(defaultTx)));
    }

    public <R> R transactionWithRetry(int maxAttempts, Duration delayBetweenAttempts, IsolationLevel isolationLevel,
            TransactionWork<Transaction, R> work) throws SQLException {
        return delegate.transactionWithRetry(maxAttempts, delayBetweenAttempts, isolationLevel,
                defaultTx -> work.run(new Transaction(defaultTx)));
    }

    public static final class Transaction {
        private final DefaultClient.Transaction delegate;

        private Transaction(DefaultClient.Transaction delegate) {
            this.delegate = delegate;
        }

        /// REQUIRED — runs `work` against this same transaction; the enclosing `transaction(...)`
        /// call still owns the commit/rollback boundary.
        public <R> R transaction(TransactionWork<Transaction, R> work) throws SQLException {
            return delegate.transaction(defaultTx -> work.run(new Transaction(defaultTx)));
        }

        public void transactionWithoutResult(TransactionAction<Transaction> action) throws SQLException {
            transaction(tx -> {
                action.run(tx);
                return null;
            });
        }

        /// REQUIRES_NEW — runs `work` in its own transaction on a separate connection, committed
        /// or rolled back independently of this one.
        public <R> R transactionRequiringNew(TransactionWork<Transaction, R> work) throws SQLException {
            return delegate.transactionRequiringNew(defaultTx -> work.run(new Transaction(defaultTx)));
        }

        public <R> R transactionRequiringNew(IsolationLevel isolationLevel, TransactionWork<Transaction, R> work)
                throws SQLException {
            return delegate.transactionRequiringNew(isolationLevel, defaultTx -> work.run(new Transaction(defaultTx)));
        }

        public void transactionWithoutResultRequiringNew(TransactionAction<Transaction> action) throws SQLException {
            transactionRequiringNew(tx -> {
                action.run(tx);
                return null;
            });
        }

        public void transactionWithoutResultRequiringNew(IsolationLevel isolationLevel,
                TransactionAction<Transaction> action) throws SQLException {
            transactionRequiringNew(isolationLevel, tx -> {
                action.run(tx);
                return null;
            });
        }

        /// NESTED — runs `work` inside a `SAVEPOINT` on this same transaction: its writes roll
        /// back on failure without unwinding the enclosing transaction.
        public <R> R transactionNested(TransactionWork<Transaction, R> work) throws SQLException {
            return delegate.transactionNested(defaultTx -> work.run(new Transaction(defaultTx)));
        }

        public void transactionWithoutResultNested(TransactionAction<Transaction> action) throws SQLException {
            transactionNested(tx -> {
                action.run(tx);
                return null;
            });
        }

        public <T> SelectStep<T> select(Table<T> table) {
            return delegate.select(table);
        }

        public JoinStep select(AliasedTable<?> table) {
            return delegate.select(table);
        }

        public <T> ReturningInsertStep<T> insert(Table<T> table) {
            return delegate.insert(table);
        }

        public <T> ReturningUpdateStep<T> update(Table<T> table) {
            return delegate.update(table);
        }

        public <T> ReturningDeleteStep<T> delete(Table<T> table) {
            return delegate.delete(table);
        }

        /// Raw SQL, bound positionally to its `?` placeholders — `sql` is not escaped or
        /// validated; never build it by concatenating untrusted input.
        public RawStep sql(@Language("SQL") String sql, Object... params) {
            return delegate.sql(sql, params);
        }
    }
}
