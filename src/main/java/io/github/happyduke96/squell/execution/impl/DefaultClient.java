package io.github.happyduke96.squell.execution.impl;

import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.sql.AliasedTable;
import io.github.happyduke96.squell.sql.Table;

import io.github.happyduke96.squell.connection.ConnectionSource;
import io.github.happyduke96.squell.connection.IsolationLevel;
import io.github.happyduke96.squell.connection.PerCallConnection;
import io.github.happyduke96.squell.connection.SharedConnection;
import io.github.happyduke96.squell.connection.TransactionAction;
import io.github.happyduke96.squell.connection.TransactionWork;
import io.github.happyduke96.squell.exception.DataAccessException;
import io.github.happyduke96.squell.execution.JoinStep;
import io.github.happyduke96.squell.execution.LockableSelectStep;
import io.github.happyduke96.squell.execution.RawStep;
import io.github.happyduke96.squell.execution.ReturningDeleteStep;
import io.github.happyduke96.squell.execution.ReturningInsertStep;
import io.github.happyduke96.squell.execution.ReturningUpdateStep;
import io.github.happyduke96.squell.internal.SqlBuilder;
import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DefaultClient {

    private final DataSource dataSource;

    public DefaultClient(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> LockableSelectStep<T> select(Table<T> table) {
        return new DefaultSelectStep<>(table, new PerCallConnection(dataSource));
    }

    public JoinStep select(AliasedTable<?> table) {
        return new DefaultJoinStep(table, new PerCallConnection(dataSource));
    }

    public <T> ReturningInsertStep<T> insert(Table<T> table) {
        return new DefaultInsertStep<>(table, new PerCallConnection(dataSource));
    }

    public <T> ReturningUpdateStep<T> update(Table<T> table) {
        return new DefaultUpdateStep<>(table, new PerCallConnection(dataSource));
    }

    public <T> ReturningDeleteStep<T> delete(Table<T> table) {
        return new DefaultDeleteStep<>(table, new PerCallConnection(dataSource));
    }

    public <T> int[] insertBatch(Table<T> table, List<T> entities) throws SQLException {
        List<Field<?>> fields = table.insertableFields();
        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                columns.append(", ");
                placeholders.append(", ");
            }
            columns.append(fields.get(i).name());
            placeholders.append('?');
        }

        String sql = new SqlBuilder()
                .append("INSERT INTO ").append(table.name())
                .append(" (").append(columns.toString()).append(") VALUES (").append(placeholders.toString())
                .append(")")
                .toString();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (T entity : entities) {
                List<Object> values = table.insertableValues(entity);
                for (int i = 0; i < values.size(); i++) {
                    statement.setObject(i + 1, values.get(i));
                }
                statement.addBatch();
            }
            return statement.executeBatch();
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        }
    }

    public <T> int[] updateBatch(Table<T> table, List<T> entities, Field<?> matchField) throws SQLException {
        List<Field<?>> allFields = table.fields();
        StringBuilder setClause = new StringBuilder();
        for (Field<?> field : allFields) {
            if (field.name().equals(matchField.name())) {
                continue;
            }
            if (!setClause.isEmpty()) {
                setClause.append(", ");
            }
            setClause.append(field.name()).append(" = ?");
        }
        if (setClause.isEmpty()) {
            throw new IllegalArgumentException(
                    "updateBatch on [" + table.name() + "] has no columns to update besides [" + matchField.name()
                            + "].");
        }

        String sql = new SqlBuilder()
                .append("UPDATE ").append(table.name()).append(" SET ").append(setClause.toString())
                .append(" WHERE ").append(matchField.name()).append(" = ?")
                .toString();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (T entity : entities) {
                List<Object> values = table.values(entity);
                int index = 1;
                Object matchValue = null;
                for (int i = 0; i < allFields.size(); i++) {
                    Field<?> field = allFields.get(i);
                    if (field.name().equals(matchField.name())) {
                        matchValue = values.get(i);
                    } else {
                        statement.setObject(index++, values.get(i));
                    }
                }
                statement.setObject(index, matchValue);
                statement.addBatch();
            }
            return statement.executeBatch();
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        }
    }

    public RawStep sql(@Language("SQL") String sql, Object... params) {
        return new RawStep(sql, params, new PerCallConnection(dataSource));
    }

    public List<String> validateSchema(Table<?>... tables) throws SQLException {
        return validateSchema(List.of(tables));
    }

    public List<String> validateSchema(Collection<Table<?>> tables) throws SQLException {
        List<String> issues = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            for (Table<?> table : tables) {
                Map<String, Boolean> nullableByColumn = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                boolean tableExists = false;
                try (ResultSet columns = metadata.getColumns(null, null, table.name(), null)) {
                    while (columns.next()) {
                        tableExists = true;
                        nullableByColumn.put(columns.getString("COLUMN_NAME"),
                                "YES".equals(columns.getString("IS_NULLABLE")));
                    }
                }
                if (!tableExists) {
                    issues.add("Table [" + table.name() + "] does not exist.");
                    continue;
                }
                Set<String> singleColumnUniqueColumns = singleColumnUniqueIndexes(metadata, table.name());
                for (Field<?> field : table.fields()) {
                    Boolean nullable = nullableByColumn.get(field.name());
                    if (nullable == null) {
                        issues.add("Table [" + table.name() + "] is missing column [" + field.name() + "].");
                        continue;
                    }
                    if (field.nonNull() && nullable) {
                        issues.add("Table [" + table.name() + "] column [" + field.name()
                                + "] is nullable but the entity declares nonNull = true.");
                    }
                    if (field.unique() && !singleColumnUniqueColumns.contains(field.name())) {
                        issues.add("Table [" + table.name() + "] column [" + field.name()
                                + "] is expected to be unique but no single-column unique index was found.");
                    }
                }
            }
        }
        return issues;
    }

    private static Set<String> singleColumnUniqueIndexes(DatabaseMetaData metadata, String tableName)
            throws SQLException {
        Map<String, Integer> columnCountByIndex = new HashMap<>();
        Map<String, String> soleColumnByIndex = new HashMap<>();
        try (ResultSet indexInfo = metadata.getIndexInfo(null, null, tableName, true, false)) {
            while (indexInfo.next()) {
                String indexName = indexInfo.getString("INDEX_NAME");
                String columnName = indexInfo.getString("COLUMN_NAME");
                if (indexName == null || columnName == null) {
                    continue;
                }
                columnCountByIndex.merge(indexName, 1, Integer::sum);
                soleColumnByIndex.putIfAbsent(indexName, columnName);
            }
        }
        Set<String> uniqueColumns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        columnCountByIndex.forEach((indexName, count) -> {
            if (count == 1) {
                uniqueColumns.add(soleColumnByIndex.get(indexName));
            }
        });
        return uniqueColumns;
    }

    public void validateSchemaOrThrow(Table<?>... tables) throws SQLException {
        validateSchemaOrThrow(List.of(tables));
    }

    public void validateSchemaOrThrow(Collection<Table<?>> tables) throws SQLException {
        List<String> issues = validateSchema(tables);
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Schema validation failed:\n" + String.join("\n", issues));
        }
    }

    public <R> R transaction(TransactionWork<Transaction, R> work) throws SQLException {
        return runTransaction(dataSource, null, work);
    }

    public <R> R transaction(IsolationLevel isolationLevel, TransactionWork<Transaction, R> work)
            throws SQLException {
        return runTransaction(dataSource, isolationLevel, work);
    }

    /// Retries `work` in a fresh `transaction(...)` while it fails with a retryable
    /// `DataAccessException` (a deadlock, a `forUpdateNoWait()` conflict, a serialization
    /// failure...), up to `maxAttempts` attempts, waiting `delayBetweenAttempts` between them.
    public <R> R transactionWithRetry(int maxAttempts, Duration delayBetweenAttempts,
            TransactionWork<Transaction, R> work) throws SQLException {
        return transactionWithRetry(maxAttempts, delayBetweenAttempts, null, work);
    }

    public <R> R transactionWithRetry(int maxAttempts, Duration delayBetweenAttempts, IsolationLevel isolationLevel,
            TransactionWork<Transaction, R> work) throws SQLException {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, got [" + maxAttempts + "].");
        }
        for (int attempt = 1; ; attempt++) {
            try {
                return runTransaction(dataSource, isolationLevel, work);
            } catch (DataAccessException e) {
                if (!e.retryable() || attempt >= maxAttempts) {
                    throw e;
                }
                sleep(delayBetweenAttempts);
            }
        }
    }

    private static void sleep(Duration duration) throws SQLException {
        if (duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting to retry a transaction.", e);
        }
    }

    private static <R> R runTransaction(DataSource dataSource, IsolationLevel isolationLevel,
            TransactionWork<Transaction, R> work) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (isolationLevel != null) {
                connection.setTransactionIsolation(isolationLevel.jdbcLevel());
            }
            connection.setAutoCommit(false);
            try {
                R result = work.run(new Transaction(connection, dataSource));
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection, e);
                throw e;
            } catch (Error e) {
                rollbackQuietly(connection, e);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static void rollbackQuietly(Connection connection, Throwable cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }

    private static void rollbackToSavepointQuietly(Connection connection, Savepoint savepoint, Throwable cause) {
        try {
            connection.rollback(savepoint);
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }

    public static final class Transaction {
        private final ConnectionSource source;
        private final DataSource dataSource;

        private Transaction(Connection connection, DataSource dataSource) {
            this.source = new SharedConnection(connection);
            this.dataSource = dataSource;
        }

        public <T> LockableSelectStep<T> select(Table<T> table) {
            return new DefaultSelectStep<>(table, source);
        }

        public JoinStep select(AliasedTable<?> table) {
            return new DefaultJoinStep(table, source);
        }

        public <T> ReturningInsertStep<T> insert(Table<T> table) {
            return new DefaultInsertStep<>(table, source);
        }

        public <T> ReturningUpdateStep<T> update(Table<T> table) {
            return new DefaultUpdateStep<>(table, source);
        }

        public <T> ReturningDeleteStep<T> delete(Table<T> table) {
            return new DefaultDeleteStep<>(table, source);
        }

        public RawStep sql(@Language("SQL") String sql, Object... params) {
            return new RawStep(sql, params, source);
        }

        /// REQUIRED — runs `work` against this same connection; the enclosing `transaction(...)`
        /// call still owns the commit/rollback boundary.
        public <R> R transaction(TransactionWork<Transaction, R> work) throws SQLException {
            return work.run(this);
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
            return runTransaction(dataSource, null, work);
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

        /// NESTED — runs `work` inside a `SAVEPOINT` on this same connection: its writes roll
        /// back on failure without unwinding the enclosing transaction.
        public <R> R transactionNested(TransactionWork<Transaction, R> work) throws SQLException {
            Connection connection = source.acquire();
            Savepoint savepoint = connection.setSavepoint();
            try {
                R result = work.run(this);
                connection.releaseSavepoint(savepoint);
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackToSavepointQuietly(connection, savepoint, e);
                throw e;
            } catch (Error e) {
                rollbackToSavepointQuietly(connection, savepoint, e);
                throw e;
            } finally {
                source.release(connection);
            }
        }

        public void transactionWithoutResultNested(TransactionAction<Transaction> action) throws SQLException {
            transactionNested(tx -> {
                action.run(tx);
                return null;
            });
        }

        public <R> R transactionRequiringNew(IsolationLevel isolationLevel, TransactionWork<Transaction, R> work)
                throws SQLException {
            return runTransaction(dataSource, isolationLevel, work);
        }
    }
}
