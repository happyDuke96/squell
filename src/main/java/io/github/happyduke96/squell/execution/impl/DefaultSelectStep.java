package io.github.happyduke96.squell.execution.impl;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.condition.NoCondition;
import io.github.happyduke96.squell.condition.SubQuery;
import io.github.happyduke96.squell.sql.Table;

import io.github.happyduke96.squell.connection.ConnectionSource;
import io.github.happyduke96.squell.exception.DataAccessException;
import io.github.happyduke96.squell.exception.UncheckedSqlException;
import io.github.happyduke96.squell.execution.LockableSelectStep;
import io.github.happyduke96.squell.execution.NoRowBound;
import io.github.happyduke96.squell.execution.Page;
import io.github.happyduke96.squell.execution.RowBound;
import io.github.happyduke96.squell.execution.SelectStep;
import io.github.happyduke96.squell.execution.Slice;
import io.github.happyduke96.squell.execution.SomeRowBound;
import io.github.happyduke96.squell.internal.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class DefaultSelectStep<T> implements LockableSelectStep<T> {

    private record OrderBy(Field<?> field, boolean descending) {
    }

    private final Table<T> table;
    private final ConnectionSource source;
    private final Condition condition;
    private final List<OrderBy> orderBy;
    private final RowBound limit;
    private final RowBound offset;
    private final boolean distinct;
    private final String lockClause;

    public DefaultSelectStep(Table<T> table, ConnectionSource source) {
        this(table, source, NoCondition.INSTANCE, List.of(), NoRowBound.INSTANCE, NoRowBound.INSTANCE, false, "");
    }

    private DefaultSelectStep(Table<T> table, ConnectionSource source, Condition condition, List<OrderBy> orderBy,
            RowBound limit, RowBound offset, boolean distinct, String lockClause) {
        this.table = table;
        this.source = source;
        this.condition = condition;
        this.orderBy = orderBy;
        this.limit = limit;
        this.offset = offset;
        this.distinct = distinct;
        this.lockClause = lockClause;
    }

    @Override
    public DefaultSelectStep<T> where(Condition condition) {
        return new DefaultSelectStep<>(table, source, this.condition.and(condition), orderBy, limit, offset,
                distinct, lockClause);
    }

    @Override
    public DefaultSelectStep<T> orderBy(Field<?> field) {
        List<OrderBy> next = new ArrayList<>(orderBy);
        next.add(new OrderBy(field, false));
        return new DefaultSelectStep<>(table, source, condition, next, limit, offset, distinct, lockClause);
    }

    @Override
    public DefaultSelectStep<T> orderByDesc(Field<?> field) {
        List<OrderBy> next = new ArrayList<>(orderBy);
        next.add(new OrderBy(field, true));
        return new DefaultSelectStep<>(table, source, condition, next, limit, offset, distinct, lockClause);
    }

    @Override
    public DefaultSelectStep<T> limit(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative, got [" + limit + "].");
        }
        return new DefaultSelectStep<>(table, source, condition, orderBy, new SomeRowBound("LIMIT", limit), offset,
                distinct, lockClause);
    }

    @Override
    public DefaultSelectStep<T> offset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative, got [" + offset + "].");
        }
        return new DefaultSelectStep<>(table, source, condition, orderBy, limit, new SomeRowBound("OFFSET", offset),
                distinct, lockClause);
    }

    @Override
    public DefaultSelectStep<T> distinct() {
        return new DefaultSelectStep<>(table, source, condition, orderBy, limit, offset, true, lockClause);
    }

    @Override
    public SelectStep<T> forUpdate() {
        return new DefaultSelectStep<>(table, source, condition, orderBy, limit, offset, distinct, "FOR UPDATE");
    }

    @Override
    public SelectStep<T> forUpdateSkipLocked() {
        return new DefaultSelectStep<>(table, source, condition, orderBy, limit, offset, distinct,
                "FOR UPDATE SKIP LOCKED");
    }

    @Override
    public SelectStep<T> forUpdateNoWait() {
        return new DefaultSelectStep<>(table, source, condition, orderBy, limit, offset, distinct,
                "FOR UPDATE NOWAIT");
    }

    @Override
    public SelectStep<T> forShare() {
        return new DefaultSelectStep<>(table, source, condition, orderBy, limit, offset, distinct, "FOR SHARE");
    }

    @Override
    public List<T> fetch() throws SQLException {
        String sql = selectAllQuery();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement);
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

    @Override
    public Stream<T> fetchStream() throws SQLException {
        String sql = selectAllQuery();
        Connection connection = source.acquire();
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            bind(statement);
            ResultSet rows = statement.executeQuery();
            PreparedStatement openStatement = statement;
            return StreamSupport.stream(new ResultSetSpliterator<>(rows, table), false)
                    .onClose(() -> release(connection, openStatement, rows));
        } catch (SQLException e) {
            closeQuietly(statement);
            source.release(connection);
            throw DataAccessException.translate(e);
        }
    }

    private String selectAllQuery() {
        return new SqlBuilder()
                .append("SELECT ")
                .appendIf(distinct, "DISTINCT ")
                .append("* FROM ").append(table.name())
                .clause("WHERE", condition.sql())
                .clause("ORDER BY", orderByContent())
                .append(limit.sql())
                .append(offset.sql())
                .appendIf(!lockClause.isEmpty(), " " + lockClause)
                .toString();
    }

    private void release(Connection connection, PreparedStatement statement, ResultSet rows) {
        try {
            rows.close();
        } catch (SQLException ignored) {
            // Best-effort close on an already-exhausted query.
        }
        closeQuietly(statement);
        try {
            source.release(connection);
        } catch (SQLException e) {
            throw new UncheckedSqlException(e);
        }
    }

    private static void closeQuietly(PreparedStatement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException ignored) {
            // Best-effort close on an already-failed or already-exhausted query.
        }
    }

    private static final class ResultSetSpliterator<T> extends Spliterators.AbstractSpliterator<T> {
        private final ResultSet rows;
        private final Table<T> table;

        private ResultSetSpliterator(ResultSet rows, Table<T> table) {
            super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL);
            this.rows = rows;
            this.table = table;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            try {
                if (!rows.next()) {
                    return false;
                }
                action.accept(table.fromRow(rows));
                return true;
            } catch (SQLException e) {
                throw new UncheckedSqlException(e);
            }
        }
    }

    @Override
    public Optional<T> fetchOne() throws SQLException {
        List<T> results = fetch();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Query for [" + table.name() + "] returned more than one row.");
        }
        return Optional.of(results.getFirst());
    }

    @Override
    public <V> List<V> fetchColumn(Field<V> field) throws SQLException {
        String sql = new SqlBuilder()
                .append("SELECT ")
                .appendIf(distinct, "DISTINCT ")
                .append(field.name()).append(" FROM ").append(table.name())
                .clause("WHERE", condition.sql())
                .clause("ORDER BY", orderByContent())
                .append(limit.sql())
                .append(offset.sql())
                .appendIf(!lockClause.isEmpty(), " " + lockClause)
                .toString();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement);
                try (ResultSet rows = statement.executeQuery()) {
                    List<V> results = new ArrayList<>();
                    while (rows.next()) {
                        @SuppressWarnings("unchecked")
                        V value = (V) field.readFrom(rows);
                        results.add(value);
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

    @Override
    public long count() throws SQLException {
        String sql = new SqlBuilder()
                .append("SELECT COUNT(*) FROM ").append(table.name())
                .clause("WHERE", condition.sql())
                .toString();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindValues(statement, condition.values(), 1);
                try (ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    return rows.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    @Override
    public Page<T> fetchPage(int pageNumber, int pageSize) throws SQLException {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative, got [" + pageNumber + "].");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive, got [" + pageSize + "].");
        }
        List<T> content = limit(pageSize).offset(pageNumber * pageSize).fetch();
        return new Page<>(content, count(), pageNumber, pageSize);
    }

    @Override
    public Slice<T> fetchSlice(int limit) throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, got [" + limit + "].");
        }
        List<T> fetched = limit(limit + 1).fetch();
        boolean hasNext = fetched.size() > limit;
        return new Slice<>(hasNext ? fetched.subList(0, limit) : fetched, hasNext);
    }

    @Override
    public <V> Optional<V> min(Field<V> field) throws SQLException {
        return minMax("MIN", field);
    }

    @Override
    public <V> Optional<V> max(Field<V> field) throws SQLException {
        return minMax("MAX", field);
    }

    @Override
    public <N extends Number> Optional<Double> sum(Field<N> field) throws SQLException {
        return numericAggregate("SUM", field.name());
    }

    @Override
    public <N extends Number> Optional<Double> avg(Field<N> field) throws SQLException {
        return numericAggregate("AVG", field.name());
    }

    @Override
    public <K> Map<K, Long> countBy(Field<K> groupField) throws SQLException {
        return countBy(groupField, NoCondition.INSTANCE);
    }

    @Override
    public <K> Map<K, Long> countBy(Field<K> groupField, Condition having) throws SQLException {
        String sql = new SqlBuilder()
                .append("SELECT ").append(groupField.name()).append(", COUNT(*) FROM ").append(table.name())
                .clause("WHERE", condition.sql())
                .append(" GROUP BY ").append(groupField.name())
                .clause("HAVING", having.sql())
                .toString();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindGrouped(statement, having);
                try (ResultSet rows = statement.executeQuery()) {
                    Map<K, Long> result = new LinkedHashMap<>();
                    while (rows.next()) {
                        @SuppressWarnings("unchecked")
                        K key = (K) groupField.readFrom(rows);
                        result.put(key, rows.getLong(2));
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    @Override
    public <K, V> Map<K, V> minBy(Field<K> groupField, Field<V> field) throws SQLException {
        return minMaxBy("MIN", groupField, field, NoCondition.INSTANCE);
    }

    @Override
    public <K, V> Map<K, V> minBy(Field<K> groupField, Field<V> field, Condition having) throws SQLException {
        return minMaxBy("MIN", groupField, field, having);
    }

    @Override
    public <K, V> Map<K, V> maxBy(Field<K> groupField, Field<V> field) throws SQLException {
        return minMaxBy("MAX", groupField, field, NoCondition.INSTANCE);
    }

    @Override
    public <K, V> Map<K, V> maxBy(Field<K> groupField, Field<V> field, Condition having) throws SQLException {
        return minMaxBy("MAX", groupField, field, having);
    }

    @Override
    public <K, N extends Number> Map<K, Double> sumBy(Field<K> groupField, Field<N> field) throws SQLException {
        return numericAggregateBy("SUM", groupField, field.name(), NoCondition.INSTANCE);
    }

    @Override
    public <K, N extends Number> Map<K, Double> sumBy(Field<K> groupField, Field<N> field, Condition having)
            throws SQLException {
        return numericAggregateBy("SUM", groupField, field.name(), having);
    }

    @Override
    public <K, N extends Number> Map<K, Double> avgBy(Field<K> groupField, Field<N> field) throws SQLException {
        return numericAggregateBy("AVG", groupField, field.name(), NoCondition.INSTANCE);
    }

    @Override
    public <K, N extends Number> Map<K, Double> avgBy(Field<K> groupField, Field<N> field, Condition having)
            throws SQLException {
        return numericAggregateBy("AVG", groupField, field.name(), having);
    }

    private <V> Optional<V> minMax(String function, Field<V> field) throws SQLException {
        String sql = new SqlBuilder()
                .append("SELECT ").append(function).append("(").append(field.name()).append(") AS ")
                .append(field.name()).append(" FROM ").append(table.name())
                .clause("WHERE", condition.sql())
                .toString();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindValues(statement, condition.values(), 1);
                try (ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    @SuppressWarnings("unchecked")
                    V value = (V) field.readFrom(rows);
                    return Optional.ofNullable(value);
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    private Optional<Double> numericAggregate(String function, String columnName) throws SQLException {
        String sql = new SqlBuilder()
                .append("SELECT ").append(function).append("(").append(columnName).append(") FROM ")
                .append(table.name())
                .clause("WHERE", condition.sql())
                .toString();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindValues(statement, condition.values(), 1);
                try (ResultSet rows = statement.executeQuery()) {
                    rows.next();
                    Double value = (Double) rows.getObject(1, Double.class);
                    return Optional.ofNullable(value);
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    private <K, V> Map<K, V> minMaxBy(String function, Field<K> groupField, Field<V> field, Condition having)
            throws SQLException {
        String sql = new SqlBuilder()
                .append("SELECT ").append(groupField.name()).append(", ").append(function).append("(")
                .append(field.name()).append(") AS ").append(field.name())
                .append(" FROM ").append(table.name())
                .clause("WHERE", condition.sql())
                .append(" GROUP BY ").append(groupField.name())
                .clause("HAVING", having.sql())
                .toString();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindGrouped(statement, having);
                try (ResultSet rows = statement.executeQuery()) {
                    Map<K, V> result = new LinkedHashMap<>();
                    while (rows.next()) {
                        @SuppressWarnings("unchecked")
                        K key = (K) groupField.readFrom(rows);
                        @SuppressWarnings("unchecked")
                        V value = (V) field.readFrom(rows);
                        result.put(key, value);
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    private <K> Map<K, Double> numericAggregateBy(String function, Field<K> groupField, String columnName,
            Condition having) throws SQLException {
        String sql = new SqlBuilder()
                .append("SELECT ").append(groupField.name()).append(", ").append(function).append("(")
                .append(columnName).append(") FROM ").append(table.name())
                .clause("WHERE", condition.sql())
                .append(" GROUP BY ").append(groupField.name())
                .clause("HAVING", having.sql())
                .toString();
        Connection connection = source.acquire();
        try {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindGrouped(statement, having);
                try (ResultSet rows = statement.executeQuery()) {
                    Map<K, Double> result = new LinkedHashMap<>();
                    while (rows.next()) {
                        @SuppressWarnings("unchecked")
                        K key = (K) groupField.readFrom(rows);
                        Double value = (Double) rows.getObject(2, Double.class);
                        result.put(key, value);
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    private void bindGrouped(PreparedStatement statement, Condition having) throws SQLException {
        int index = bindValues(statement, condition.values(), 1);
        bindValues(statement, having.values(), index);
    }

    @Override
    public <V> SubQuery<V> subQuery(Field<V> field) {
        String text = new SqlBuilder()
                .append("SELECT ")
                .appendIf(distinct, "DISTINCT ")
                .append(field.name()).append(" FROM ").append(table.name())
                .clause("WHERE", condition.sql())
                .clause("ORDER BY", orderByContent())
                .append(limit.sql())
                .append(offset.sql())
                .toString();
        List<Object> values = new ArrayList<>(condition.values());
        values.addAll(limit.values());
        values.addAll(offset.values());
        return new SubQuery<>(text, values);
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
        int index = bindValues(statement, condition.values(), 1);
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
