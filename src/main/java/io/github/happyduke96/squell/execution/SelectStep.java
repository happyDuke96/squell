package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.condition.SubQuery;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface SelectStep<T> {

    SelectStep<T> where(Condition condition);

    default SelectStep<T> whereIf(boolean test, Supplier<Condition> condition) {
        return test ? where(condition.get()) : this;
    }

    SelectStep<T> orderBy(Field<?> field);

    SelectStep<T> orderByDesc(Field<?> field);

    SelectStep<T> limit(int limit);

    SelectStep<T> offset(int offset);

    SelectStep<T> distinct();

    List<T> fetch() throws SQLException;

    /**
     * Returns a lazy stream over the result set, for results too large to hold in memory.
     *
     * <p>The stream keeps the connection and result set open: consume it in try-with-resources, or
     * the connection leaks.
     *
     * @return the stream of rows
     * @throws SQLException if the query fails
     */
    Stream<T> fetchStream() throws SQLException;

    Optional<T> fetchOne() throws SQLException;

    <V> List<V> fetchColumn(Field<V> field) throws SQLException;

    long count() throws SQLException;

    /**
     * Fetches one page of rows together with the total row count.
     *
     * @param pageNumber the 0-based page index
     * @param pageSize the rows per page
     * @return the page
     * @throws SQLException if the query fails
     */
    Page<T> fetchPage(int pageNumber, int pageSize) throws SQLException;

    /**
     * Fetches up to {@code limit} rows without {@code COUNT(*)} or {@code OFFSET}.
     *
     * <p>Combine it with {@code where(cursor.gt(lastSeen))} and {@code orderBy(cursor)} for keyset
     * pagination that stays fast at any depth.
     *
     * @param limit the maximum number of rows
     * @return the slice
     * @throws SQLException if the query fails
     */
    Slice<T> fetchSlice(int limit) throws SQLException;

    <V> Optional<V> min(Field<V> field) throws SQLException;

    <V> Optional<V> max(Field<V> field) throws SQLException;

    <N extends Number> Optional<Double> sum(Field<N> field) throws SQLException;

    <N extends Number> Optional<Double> avg(Field<N> field) throws SQLException;

    <K> Map<K, Long> countBy(Field<K> groupField) throws SQLException;

    <K> Map<K, Long> countBy(Field<K> groupField, Condition having) throws SQLException;

    <K, V> Map<K, V> minBy(Field<K> groupField, Field<V> field) throws SQLException;

    <K, V> Map<K, V> minBy(Field<K> groupField, Field<V> field, Condition having) throws SQLException;

    <K, V> Map<K, V> maxBy(Field<K> groupField, Field<V> field) throws SQLException;

    <K, V> Map<K, V> maxBy(Field<K> groupField, Field<V> field, Condition having) throws SQLException;

    <K, N extends Number> Map<K, Double> sumBy(Field<K> groupField, Field<N> field) throws SQLException;

    <K, N extends Number> Map<K, Double> sumBy(Field<K> groupField, Field<N> field, Condition having) throws SQLException;

    <K, N extends Number> Map<K, Double> avgBy(Field<K> groupField, Field<N> field) throws SQLException;

    <K, N extends Number> Map<K, Double> avgBy(Field<K> groupField, Field<N> field, Condition having) throws SQLException;

    <V> SubQuery<V> subQuery(Field<V> field);
}
