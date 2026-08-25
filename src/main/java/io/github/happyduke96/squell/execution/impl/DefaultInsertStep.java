package io.github.happyduke96.squell.execution.impl;

import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.sql.Table;

import io.github.happyduke96.squell.connection.ConnectionSource;
import io.github.happyduke96.squell.exception.DataAccessException;
import io.github.happyduke96.squell.execution.OnConflictStep;
import io.github.happyduke96.squell.execution.ReturningFilledInsertStep;
import io.github.happyduke96.squell.execution.ReturningInsertStep;
import io.github.happyduke96.squell.internal.SqlBuilder;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class DefaultInsertStep<T> implements ReturningInsertStep<T> {

    private final Table<T> table;
    private final ConnectionSource source;

    public DefaultInsertStep(Table<T> table, ConnectionSource source) {
        this.table = table;
        this.source = source;
    }

    @Override
    public ReturningFilledInsertStep<T> values(T entity) {
        return new DefaultFilledInsertStep<>(table, entity, source);
    }

    private static final class DefaultFilledInsertStep<T> implements ReturningFilledInsertStep<T> {

        private final Table<T> table;
        private final T entity;
        private final ConnectionSource source;

        private DefaultFilledInsertStep(Table<T> table, T entity, ConnectionSource source) {
            this.table = table;
            this.entity = entity;
            this.source = source;
        }

        @Override
        public void execute() throws SQLException {
            Connection connection = source.acquire();
            try {
                try (PreparedStatement statement = connection.prepareStatement(insertQuery())) {
                    bindInsertable(statement);
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                throw DataAccessException.translate(e);
            } finally {
                source.release(connection);
            }
        }

        @Override
        public T executeAndReturn() throws SQLException {
            return runReturningOne(new SqlBuilder().append(insertQuery()).append(" RETURNING *").toString());
        }

        @Override
        public OnConflictStep<T> onConflict(Field<?>... conflictTargets) {
            return new DefaultOnConflictStep(conflictTargets);
        }

        @Language("SQL")
        private String insertQuery() {
            String columns = table.insertableFields().stream().map(Field::name).collect(Collectors.joining(", "));
            String placeholders = table.insertableFields().stream().map(f -> "?").collect(Collectors.joining(", "));
            return new SqlBuilder()
                    .append("INSERT INTO ").append(table.name())
                    .append(" (").append(columns).append(") VALUES (").append(placeholders).append(")")
                    .toString();
        }

        private void bindInsertable(PreparedStatement statement) throws SQLException {
            List<Object> values = table.insertableValues(entity);
            for (int i = 0; i < values.size(); i++) {
                statement.setObject(i + 1, values.get(i));
            }
        }

        private T runReturningOne(String sql) throws SQLException {
            Connection connection = source.acquire();
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindInsertable(statement);
                    try (ResultSet rows = statement.executeQuery()) {
                        rows.next();
                        return table.fromRow(rows);
                    }
                }
            } catch (SQLException e) {
                throw DataAccessException.translate(e);
            } finally {
                source.release(connection);
            }
        }

        private final class DefaultOnConflictStep implements OnConflictStep<T> {
            private final Field<?>[] conflictTargets;

            private DefaultOnConflictStep(Field<?>[] conflictTargets) {
                this.conflictTargets = conflictTargets;
            }

            @Override
            public T doUpdate() throws SQLException {
                Set<String> conflictNames = Arrays.stream(conflictTargets).map(Field::name).collect(Collectors.toSet());
                Field<?>[] columnsToUpdate = table.insertableFields().stream()
                        .filter(f -> !conflictNames.contains(f.name()))
                        .toArray(Field<?>[]::new);
                if (columnsToUpdate.length == 0) {
                    throw new IllegalStateException("onConflict on [" + table.name()
                            + "] has nothing to update besides the conflict target " + conflictNames + ".");
                }
                return doUpdate(columnsToUpdate);
            }

            @Override
            public T doUpdate(Field<?>... columnsToUpdate) throws SQLException {
                if (columnsToUpdate.length == 0) {
                    throw new IllegalArgumentException(
                            "upsert on [" + table.name() + "] requires at least one column to update.");
                }
                String conflictColumns = Arrays.stream(conflictTargets).map(Field::name).collect(Collectors.joining(", "));
                String updateSet = Arrays.stream(columnsToUpdate)
                        .map(f -> f.name() + " = EXCLUDED." + f.name())
                        .collect(Collectors.joining(", "));
                @Language("SQL")
                String sql = new SqlBuilder()
                        .append(insertQuery())
                        .append(" ON CONFLICT (").append(conflictColumns).append(") DO UPDATE SET ").append(updateSet)
                        .append(" RETURNING *")
                        .toString();
                return runReturningOne(sql);
            }

            @Override
            public Optional<T> doNothing() throws SQLException {
                String conflictColumns = Arrays.stream(conflictTargets).map(Field::name).collect(Collectors.joining(", "));
                @Language("SQL")
                String sql = new SqlBuilder()
                        .append(insertQuery())
                        .append(" ON CONFLICT (").append(conflictColumns).append(") DO NOTHING RETURNING *")
                        .toString();

                Connection connection = source.acquire();
                try {
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        bindInsertable(statement);
                        try (ResultSet rows = statement.executeQuery()) {
                            return rows.next() ? Optional.of(table.fromRow(rows)) : Optional.empty();
                        }
                    }
                } catch (SQLException e) {
                    throw DataAccessException.translate(e);
                } finally {
                    source.release(connection);
                }
            }
        }
    }
}
