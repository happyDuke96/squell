package io.github.happyduke96.squell.execution.impl;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.condition.NoCondition;
import io.github.happyduke96.squell.sql.Table;

import io.github.happyduke96.squell.connection.ConnectionSource;
import io.github.happyduke96.squell.exception.DataAccessException;
import io.github.happyduke96.squell.execution.ReturningUpdateStep;
import io.github.happyduke96.squell.internal.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultUpdateStep<T> implements ReturningUpdateStep<T> {

    private record Assignment(String columnName, String sqlExpression, List<Object> boundValues) {
    }

    private final Table<T> table;
    private final ConnectionSource source;
    private final List<Assignment> assignments;
    private final Condition condition;

    public DefaultUpdateStep(Table<T> table, ConnectionSource source) {
        this(table, source, List.of(), NoCondition.INSTANCE);
    }

    private DefaultUpdateStep(Table<T> table, ConnectionSource source, List<Assignment> assignments,
            Condition condition) {
        this.table = table;
        this.source = source;
        this.assignments = assignments;
        this.condition = condition;
    }

    @Override
    public <V> DefaultUpdateStep<T> set(Field<V> field, V value) {
        List<Assignment> next = new ArrayList<>(assignments);
        next.add(new Assignment(field.name(), "?", Collections.singletonList(field.toSqlValue(value))));
        return new DefaultUpdateStep<>(table, source, next, condition);
    }

    @Override
    public <V> DefaultUpdateStep<T> set(Field<V> field, Field<V> other) {
        List<Assignment> next = new ArrayList<>(assignments);
        next.add(new Assignment(field.name(), other.name(), List.of()));
        return new DefaultUpdateStep<>(table, source, next, condition);
    }

    @Override
    public <N extends Number> DefaultUpdateStep<T> increment(Field<N> field, N delta) {
        List<Assignment> next = new ArrayList<>(assignments);
        next.add(new Assignment(field.name(), field.name() + " + ?", List.of(field.toSqlValue(delta))));
        return new DefaultUpdateStep<>(table, source, next, condition);
    }

    @Override
    public DefaultUpdateStep<T> where(Condition condition) {
        return new DefaultUpdateStep<>(table, source, assignments, this.condition.and(condition));
    }

    @Override
    public int execute() throws SQLException {
        requireAssignments();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(updateQuery())) {
                bindAssignments(statement);
                return statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    @Override
    public List<T> updateAndReturn() throws SQLException {
        requireAssignments();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(updateQuery() + " RETURNING *")) {
                bindAssignments(statement);
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

    private void requireAssignments() {
        if (assignments.isEmpty()) {
            throw new IllegalStateException("UPDATE [" + table.name() + "] requires at least one set(...) call.");
        }
    }

    private String updateQuery() {
        StringBuilder setClause = new StringBuilder();
        for (int i = 0; i < assignments.size(); i++) {
            if (i > 0) {
                setClause.append(", ");
            }
            setClause.append(assignments.get(i).columnName()).append(" = ").append(assignments.get(i).sqlExpression());
        }
        return new SqlBuilder()
                .append("UPDATE ").append(table.name()).append(" SET ").append(setClause.toString())
                .clause("WHERE", condition.sql())
                .toString();
    }

    private void bindAssignments(PreparedStatement statement) throws SQLException {
        int index = 1;
        for (Assignment assignment : assignments) {
            for (Object value : assignment.boundValues()) {
                statement.setObject(index++, value);
            }
        }
        for (Object value : condition.values()) {
            statement.setObject(index++, value);
        }
    }
}
