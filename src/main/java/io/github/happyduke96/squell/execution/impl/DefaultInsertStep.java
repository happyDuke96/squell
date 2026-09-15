package io.github.happyduke96.squell.execution.impl;

import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.sql.Table;

import io.github.happyduke96.squell.connection.ConnectionSource;
import io.github.happyduke96.squell.exception.DataAccessException;
import io.github.happyduke96.squell.execution.OnConflictStep;
import io.github.happyduke96.squell.execution.ReturningFilledInsertStep;
import io.github.happyduke96.squell.execution.ReturningInsertStep;
import io.github.happyduke96.squell.internal.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

        private String insertQuery() {
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
            return new SqlBuilder()
                    .append("INSERT INTO ").append(table.name())
                    .append(" (").append(columns.toString()).append(") VALUES (").append(placeholders.toString())
                    .append(")")
                    .toString();
        }

        private int bindInsertable(PreparedStatement statement) throws SQLException {
            List<Object> values = table.insertableValues(entity);
            for (int i = 0; i < values.size(); i++) {
                statement.setObject(i + 1, values.get(i));
            }
            return values.size() + 1;
        }

        private T runReturningOne(String sql, Object... extraParams) throws SQLException {
            Connection connection = source.acquire();
            try {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    int index = bindInsertable(statement);
                    for (Object param : extraParams) {
                        statement.setObject(index++, param);
                    }
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

            private String conflictColumnsClause() {
                StringBuilder columns = new StringBuilder();
                for (int i = 0; i < conflictTargets.length; i++) {
                    if (i > 0) {
                        columns.append(", ");
                    }
                    columns.append(conflictTargets[i].name());
                }
                return columns.toString();
            }

            @Override
            public T doUpdate() throws SQLException {
                Set<String> conflictNames = new HashSet<>();
                for (Field<?> target : conflictTargets) {
                    conflictNames.add(target.name());
                }
                List<Field<?>> columnsToUpdate = new ArrayList<>();
                for (Field<?> field : table.insertableFields()) {
                    if (!conflictNames.contains(field.name())) {
                        columnsToUpdate.add(field);
                    }
                }
                if (columnsToUpdate.isEmpty()) {
                    throw new IllegalStateException("onConflict on [" + table.name()
                            + "] has nothing to update besides the conflict target " + conflictNames + ".");
                }
                return doUpdate(columnsToUpdate.toArray(new Field<?>[0]));
            }

            @Override
            public T doUpdate(Field<?>... columnsToUpdate) throws SQLException {
                if (columnsToUpdate.length == 0) {
                    throw new IllegalArgumentException(
                            "upsert on [" + table.name() + "] requires at least one column to update.");
                }
                StringBuilder updateSet = new StringBuilder();
                for (int i = 0; i < columnsToUpdate.length; i++) {
                    if (i > 0) {
                        updateSet.append(", ");
                    }
                    updateSet.append(columnsToUpdate[i].name()).append(" = EXCLUDED.").append(columnsToUpdate[i].name());
                }
                String sql = new SqlBuilder()
                        .append(insertQuery())
                        .append(" ON CONFLICT (").append(conflictColumnsClause()).append(") DO UPDATE SET ")
                        .append(updateSet.toString())
                        .append(" RETURNING *")
                        .toString();
                return runReturningOne(sql);
            }

            @Override
            public <N extends Number> T doUpdateIncrementing(Field<N> counterField, N delta) throws SQLException {
                String sql = new SqlBuilder()
                        .append(insertQuery())
                        .append(" ON CONFLICT (").append(conflictColumnsClause()).append(") DO UPDATE SET ")
                        .append(counterField.name()).append(" = ").append(table.name()).append(".")
                        .append(counterField.name()).append(" + ?")
                        .append(" RETURNING *")
                        .toString();
                return runReturningOne(sql, counterField.toSqlValue(delta));
            }

            @Override
            public Optional<T> doNothing() throws SQLException {
                String sql = new SqlBuilder()
                        .append(insertQuery())
                        .append(" ON CONFLICT (").append(conflictColumnsClause()).append(") DO NOTHING RETURNING *")
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
