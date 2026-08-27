package io.github.happyduke96.squell.execution.impl;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.NoCondition;
import io.github.happyduke96.squell.sql.Table;

import io.github.happyduke96.squell.connection.ConnectionSource;
import io.github.happyduke96.squell.exception.DataAccessException;
import io.github.happyduke96.squell.execution.ReturningDeleteStep;
import io.github.happyduke96.squell.internal.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class DefaultDeleteStep<T> implements ReturningDeleteStep<T> {

    private final Table<T> table;
    private final ConnectionSource source;
    private final Condition condition;

    public DefaultDeleteStep(Table<T> table, ConnectionSource source) {
        this(table, source, NoCondition.INSTANCE);
    }

    private DefaultDeleteStep(Table<T> table, ConnectionSource source, Condition condition) {
        this.table = table;
        this.source = source;
        this.condition = condition;
    }

    @Override
    public DefaultDeleteStep<T> where(Condition condition) {
        return new DefaultDeleteStep<>(table, source, this.condition.and(condition));
    }

    @Override
    public int execute() throws SQLException {
        requireCondition();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(deleteQuery())) {
                bindCondition(statement);
                return statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    @Override
    public List<T> deleteAndReturn() throws SQLException {
        requireCondition();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(deleteQuery() + " RETURNING *")) {
                bindCondition(statement);
                try (ResultSet rows = statement.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rows.next()) {
                        results.add(table.fromRow(rows));
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    private void requireCondition() {
        if (condition.sql().isEmpty()) {
            throw new IllegalStateException(
                    "DELETE FROM [" + table.name() + "] requires where(...) with an actual condition.");
        }
    }

    private String deleteQuery() {
        return new SqlBuilder().append("DELETE FROM ").append(table.name()).clause("WHERE", condition.sql())
                .toString();
    }

    private void bindCondition(PreparedStatement statement) throws SQLException {
        int index = 1;
        for (Object value : condition.values()) {
            statement.setObject(index++, value);
        }
    }
}
