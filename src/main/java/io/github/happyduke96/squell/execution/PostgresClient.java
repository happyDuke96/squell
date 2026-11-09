package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.sql.AliasedTable;
import io.github.happyduke96.squell.sql.DefaultTable;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Entry point for PostgreSQL: query steps, raw SQL, schema validation and transactions.
 *
 * <p>Unlike {@link MySqlClient}, {@code insert} supports {@code RETURNING} and {@code ON CONFLICT}.
 */
public final class PostgresClient {

    private final DefaultClient delegate;

    public PostgresClient(DataSource dataSource) throws SQLException {
        new DialectVerifier(dataSource, "PostgresClient", "PostgreSQL", "H2").verify();
        this.delegate = new DefaultClient(dataSource);
    }

    public <T> LockableSelectStep<T> select(Table<T> table) {
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

    /**
     * Loads the children of several parents with a single {@code IN} query.
     *
     * @param parentIds the parent keys; an empty collection skips the query
     * @param childTable the table of the child entity
     * @param childForeignKey the child column that references the parent key
     * @param childKey extracts the parent key from a child, used for grouping
     * @param <K> the parent key type
     * @param <C> the child entity type
     * @return the children grouped by parent key; parents without children are absent
     * @throws SQLException if the query fails
     * @since 0.3.0
     */
    public <K, C> Map<K, List<C>> fetchRelated(Collection<K> parentIds, Table<C> childTable,
            Field<K> childForeignKey, Function<C, K> childKey) throws SQLException {
        return delegate.fetchRelated(parentIds, childTable, childForeignKey, childKey);
    }

    public <T> int[] insertBatch(Table<T> table, List<T> entities) throws SQLException {
        return delegate.insertBatch(table, entities);
    }

    public <T> int[] updateBatch(Table<T> table, List<T> entities, Field<?> matchField) throws SQLException {
        return delegate.updateBatch(table, entities, matchField);
    }

    /**
     * Creates a raw SQL statement; {@code params} are bound positionally to the {@code ?}
     * placeholders.
     *
     * <p>{@code sql} is neither escaped nor validated: build it from literals, never from untrusted
     * input.
     *
     * @param sql the SQL text
     * @param params the values for the placeholders
     * @return a step that runs the statement
     */
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

    /**
     * Runs {@code work} in a new transaction, retrying while it fails with a retryable
     * {@code DataAccessException} (a deadlock, a {@code forUpdateNoWait()} conflict, a
     * serialization failure).
     *
     * @param maxAttempts the total number of attempts, including the first
     * @param delayBetweenAttempts the pause between attempts
     * @param work the unit of work
     * @param <R> the result type
     * @return the result of the first successful attempt
     * @throws SQLException if every attempt fails or {@code work} throws a non-retryable exception
     */
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

        /**
         * Runs {@code work} in this same transaction ({@code REQUIRED}); the enclosing
         * {@code transaction(...)} call still owns commit and rollback.
         *
         * @param work the unit of work
         * @param <R> the result type
         * @return the result of {@code work}
         * @throws SQLException if {@code work} fails
         */
        public <R> R transaction(TransactionWork<Transaction, R> work) throws SQLException {
            return delegate.transaction(defaultTx -> work.run(new Transaction(defaultTx)));
        }

        public void transactionWithoutResult(TransactionAction<Transaction> action) throws SQLException {
            transaction(tx -> {
                action.run(tx);
                return null;
            });
        }

        /**
         * Runs {@code work} in its own transaction on a separate connection ({@code REQUIRES_NEW}),
         * committed or rolled back independently of this one.
         *
         * @param work the unit of work
         * @param <R> the result type
         * @return the result of {@code work}
         * @throws SQLException if {@code work} fails
         */
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

        /**
         * Runs {@code work} inside a {@code SAVEPOINT} of this transaction ({@code NESTED}); on
         * failure only its writes roll back.
         *
         * @param work the unit of work
         * @param <R> the result type
         * @return the result of {@code work}
         * @throws SQLException if {@code work} fails
         */
        public <R> R transactionNested(TransactionWork<Transaction, R> work) throws SQLException {
            return delegate.transactionNested(defaultTx -> work.run(new Transaction(defaultTx)));
        }

        public void transactionWithoutResultNested(TransactionAction<Transaction> action) throws SQLException {
            transactionNested(tx -> {
                action.run(tx);
                return null;
            });
        }

        public <T> LockableSelectStep<T> select(Table<T> table) {
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

        /**
         * Loads the children of several parents with a single {@code IN} query.
         *
         * @param parentIds the parent keys; an empty collection skips the query
         * @param childTable the table of the child entity
         * @param childForeignKey the child column that references the parent key
         * @param childKey extracts the parent key from a child, used for grouping
         * @param <K> the parent key type
         * @param <C> the child entity type
         * @return the children grouped by parent key; parents without children are absent
         * @throws SQLException if the query fails
         * @since 0.3.0
         */
        public <K, C> Map<K, List<C>> fetchRelated(Collection<K> parentIds, Table<C> childTable,
                Field<K> childForeignKey, Function<C, K> childKey) throws SQLException {
            return delegate.fetchRelated(parentIds, childTable, childForeignKey, childKey);
        }

        /**
         * Finds an entity by id, caching the result (a miss too) for the lifetime of this
         * transaction.
         *
         * <p>{@code update} and {@code delete} on this transaction evict the entry; after
         * {@code insert} or raw {@code sql} call {@link #forget}.
         *
         * @param table the table to look in
         * @param id the primary key
         * @param <T> the entity type
         * @param <ID> the primary key type
         * @return the entity, or empty if there is no such row
         * @throws SQLException if the query fails
         * @since 0.3.0
         */
        public <T, ID> Optional<T> findCached(DefaultTable<T, ID> table, ID id) throws SQLException {
            return delegate.findCached(table, id);
        }

        /**
         * Evicts a {@link #findCached} entry; needed only after a write that bypasses
         * {@code update} and {@code delete}.
         *
         * @param table the table the entry belongs to
         * @param id the primary key
         * @since 0.3.0
         */
        public void forget(Table<?> table, Object id) {
            delegate.forget(table, id);
        }

        /**
         * Creates a raw SQL statement; {@code params} are bound positionally to the {@code ?}
         * placeholders.
         *
         * <p>{@code sql} is neither escaped nor validated: build it from literals, never from
         * untrusted input.
         *
         * @param sql the SQL text
         * @param params the values for the placeholders
         * @return a step that runs the statement
         */
        public RawStep sql(@Language("SQL") String sql, Object... params) {
            return delegate.sql(sql, params);
        }
    }
}
