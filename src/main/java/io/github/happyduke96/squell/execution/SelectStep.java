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

    /// A lazy `Stream` over the result set, for rows too many to hold in memory at once. The
    /// returned `Stream` holds the `Connection`/`ResultSet` open — always consume it inside
    /// try-with-resources, or the connection leaks.
    Stream<T> fetchStream() throws SQLException;

    Optional<T> fetchOne() throws SQLException;

    <V> List<V> fetchColumn(Field<V> field) throws SQLException;

    long count() throws SQLException;

    /// Fetches this page's rows and the total row count across every page, in one call — the
    /// common offset-pagination pattern of a `LIMIT`/`OFFSET` fetch plus a separate `count()`,
    /// bundled so callers can't forget the total. `pageNumber` is 0-based.
    Page<T> fetchPage(int pageNumber, int pageSize) throws SQLException;

    /// Fetches up to `limit` rows without `COUNT(*)` or `OFFSET`, either of which degrades on a
    /// large table — pair with `where(cursorField.gt(lastSeenValue))` and `orderBy(cursorField)`
    /// for keyset pagination that stays fast at any depth.
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
