package io.github.happyduke96.squell.execution.impl;

import io.github.happyduke96.squell.sql.AliasedTable;
import io.github.happyduke96.squell.sql.RowMapper;
import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.condition.NoCondition;

import io.github.happyduke96.squell.connection.ConnectionSource;
import io.github.happyduke96.squell.exception.DataAccessException;
import io.github.happyduke96.squell.execution.JoinStep;
import io.github.happyduke96.squell.execution.NoRowBound;
import io.github.happyduke96.squell.execution.RowBound;
import io.github.happyduke96.squell.execution.SomeRowBound;
import io.github.happyduke96.squell.internal.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DefaultJoinStep implements JoinStep {

    private record Join(boolean left, AliasedTable<?> table, Condition on) {
    }

    private record OrderBy(Field<?> field, boolean descending) {
    }

    private final AliasedTable<?> base;
    private final ConnectionSource source;
    private final List<Join> joins;
    private final Condition condition;
    private final List<OrderBy> orderBy;
    private final RowBound limit;
    private final RowBound offset;
    private final boolean distinct;

    public DefaultJoinStep(AliasedTable<?> base, ConnectionSource source) {
        this(base, source, List.of(), NoCondition.INSTANCE, List.of(), NoRowBound.INSTANCE, NoRowBound.INSTANCE, false);
    }

    private DefaultJoinStep(AliasedTable<?> base, ConnectionSource source, List<Join> joins, Condition condition,
            List<OrderBy> orderBy, RowBound limit, RowBound offset, boolean distinct) {
        this.base = base;
        this.source = source;
        this.joins = joins;
        this.condition = condition;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
        this.distinct = distinct;
    }

    @Override
    public PendingJoin join(AliasedTable<?> other) {
        return new DefaultPendingJoin(other, false);
    }

    @Override
    public PendingJoin leftJoin(AliasedTable<?> other) {
        return new DefaultPendingJoin(other, true);
    }

    private final class DefaultPendingJoin implements PendingJoin {
        private final AliasedTable<?> other;
        private final boolean left;

        private DefaultPendingJoin(AliasedTable<?> other, boolean left) {
            this.other = other;
            this.left = left;
        }

        @Override
        public JoinStep on(Condition condition) {
            List<Join> next = new ArrayList<>(joins);
            next.add(new Join(left, other, condition));
            return new DefaultJoinStep(base, source, next, DefaultJoinStep.this.condition, orderBy, limit, offset,
                    distinct);
        }
    }

    @Override
    public JoinStep where(Condition condition) {
        return new DefaultJoinStep(base, source, joins, this.condition.and(condition), orderBy, limit, offset,
                distinct);
    }

    @Override
    public JoinStep orderBy(Field<?> field) {
        List<OrderBy> next = new ArrayList<>(orderBy);
        next.add(new OrderBy(field, false));
        return new DefaultJoinStep(base, source, joins, condition, next, limit, offset, distinct);
    }

    @Override
    public JoinStep orderByDesc(Field<?> field) {
        List<OrderBy> next = new ArrayList<>(orderBy);
        next.add(new OrderBy(field, true));
        return new DefaultJoinStep(base, source, joins, condition, next, limit, offset, distinct);
    }

    @Override
    public JoinStep limit(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative, got [" + limit + "].");
        }
        return new DefaultJoinStep(base, source, joins, condition, orderBy, new SomeRowBound("LIMIT", limit),
                offset, distinct);
    }

    @Override
    public JoinStep offset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative, got [" + offset + "].");
        }
        return new DefaultJoinStep(base, source, joins, condition, orderBy, limit,
                new SomeRowBound("OFFSET", offset), distinct);
    }

    @Override
    public JoinStep distinct() {
        return new DefaultJoinStep(base, source, joins, condition, orderBy, limit, offset, true);
    }

    @Override
    public <R> List<R> fetch(RowMapper<R> mapper) throws SQLException {
        return run(query("*"), mapper);
    }

    @Override
    public <T> List<T> fetch(AliasedTable<T> only) throws SQLException {
        return run(query(only.alias() + ".*"), only.mapper());
    }

    @Override
    public <R> Optional<R> fetchOne(RowMapper<R> mapper) throws SQLException {
        return exactlyOne(fetch(mapper));
    }

    @Override
    public <T> Optional<T> fetchOne(AliasedTable<T> only) throws SQLException {
        return exactlyOne(fetch(only));
    }

    private <R> Optional<R> exactlyOne(List<R> results) {
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Query for [" + base.tableName() + "] returned more than one row.");
        }
        return Optional.of(results.getFirst());
    }

    private <R> List<R> run(String sql, RowMapper<R> mapper) throws SQLException {
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement);
                try (ResultSet rows = statement.executeQuery()) {
                    List<R> results = new ArrayList<>();
                    while (rows.next()) {
                        results.add(mapper.map(rows));
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

    private String query(String projection) {
        SqlBuilder sql = new SqlBuilder()
                .append("SELECT ")
                .appendIf(distinct, "DISTINCT ")
                .append(projection)
                .append(" FROM ").append(base.tableName()).append(" ").append(base.alias());
        for (Join join : joins) {
            sql.append(join.left() ? " LEFT JOIN " : " JOIN ")
                    .append(join.table().tableName()).append(" ").append(join.table().alias())
                    .append(" ON ").append(join.on().sql());
        }
        return sql.clause("WHERE", condition.sql())
                .clause("ORDER BY", orderByContent())
                .append(limit.sql())
                .append(offset.sql())
                .toString();
    }

    private String orderByContent() {
        if (orderBy.isEmpty()) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < orderBy.size(); i++) {
            if (i > 0) {
                content.append(", ");
            }
            OrderBy o = orderBy.get(i);
            content.append(o.field().name());
            if (o.descending()) {
                content.append(" DESC");
            }
        }
        return content.toString();
    }

    private void bind(PreparedStatement statement) throws SQLException {
        int index = 1;
        for (Join join : joins) {
            index = bindValues(statement, join.on().values(), index);
        }
        index = bindValues(statement, condition.values(), index);
        index = bindValues(statement, limit.values(), index);
        bindValues(statement, offset.values(), index);
    }

    private static int bindValues(PreparedStatement statement, List<Object> values, int startIndex)
            throws SQLException {
        int index = startIndex;
        for (Object value : values) {
            statement.setObject(index++, value);
        }
        return index;
    }
}
