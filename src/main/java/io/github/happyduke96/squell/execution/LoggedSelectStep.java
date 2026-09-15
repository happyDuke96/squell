package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.condition.SubQuery;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class LoggedSelectStep<T> implements SelectStep<T> {

    private static final System.Logger LOGGER = System.getLogger(LoggedSelectStep.class.getName());

    private final SelectStep<T> origin;
    private final System.Logger.Level level;

    public LoggedSelectStep(SelectStep<T> origin) {
        this(origin, System.Logger.Level.DEBUG);
    }

    public LoggedSelectStep(SelectStep<T> origin, System.Logger.Level level) {
        this.origin = origin;
        this.level = level;
    }

    @Override
    public SelectStep<T> where(Condition condition) {
        return new LoggedSelectStep<>(origin.where(condition), level);
    }

    @Override
    public SelectStep<T> orderBy(Field<?> field) {
        return new LoggedSelectStep<>(origin.orderBy(field), level);
    }

    @Override
    public SelectStep<T> orderByDesc(Field<?> field) {
        return new LoggedSelectStep<>(origin.orderByDesc(field), level);
    }

    @Override
    public SelectStep<T> limit(int limit) {
        return new LoggedSelectStep<>(origin.limit(limit), level);
    }

    @Override
    public SelectStep<T> offset(int offset) {
        return new LoggedSelectStep<>(origin.offset(offset), level);
    }

    @Override
    public SelectStep<T> distinct() {
        return new LoggedSelectStep<>(origin.distinct(), level);
    }

    @Override
    public List<T> fetch() throws SQLException {
        LOGGER.log(level, "Fetching through a decorated select step");
        return origin.fetch();
    }

    @Override
    public Stream<T> fetchStream() throws SQLException {
        LOGGER.log(level, "Fetching through a decorated select step");
        return origin.fetchStream();
    }

    @Override
    public Optional<T> fetchOne() throws SQLException {
        LOGGER.log(level, "Fetching through a decorated select step");
        return origin.fetchOne();
    }

    @Override
    public <V> List<V> fetchColumn(Field<V> field) throws SQLException {
        LOGGER.log(level, "Fetching through a decorated select step");
        return origin.fetchColumn(field);
    }

    @Override
    public long count() throws SQLException {
        LOGGER.log(level, "Fetching through a decorated select step");
        return origin.count();
    }

    @Override
    public Page<T> fetchPage(int pageNumber, int pageSize) throws SQLException {
        LOGGER.log(level, "Fetching through a decorated select step");
        return origin.fetchPage(pageNumber, pageSize);
    }

    @Override
    public Slice<T> fetchSlice(int limit) throws SQLException {
        LOGGER.log(level, "Fetching through a decorated select step");
        return origin.fetchSlice(limit);
    }

    @Override
    public <V> Optional<V> min(Field<V> field) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.min(field);
    }

    @Override
    public <V> Optional<V> max(Field<V> field) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.max(field);
    }

    @Override
    public <N extends Number> Optional<Double> sum(Field<N> field) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.sum(field);
    }

    @Override
    public <N extends Number> Optional<Double> avg(Field<N> field) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.avg(field);
    }

    @Override
    public <K> Map<K, Long> countBy(Field<K> groupField) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.countBy(groupField);
    }

    @Override
    public <K> Map<K, Long> countBy(Field<K> groupField, Condition having) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.countBy(groupField, having);
    }

    @Override
    public <K, V> Map<K, V> minBy(Field<K> groupField, Field<V> field) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.minBy(groupField, field);
    }

    @Override
    public <K, V> Map<K, V> minBy(Field<K> groupField, Field<V> field, Condition having) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.minBy(groupField, field, having);
    }

    @Override
    public <K, V> Map<K, V> maxBy(Field<K> groupField, Field<V> field) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.maxBy(groupField, field);
    }

    @Override
    public <K, V> Map<K, V> maxBy(Field<K> groupField, Field<V> field, Condition having) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.maxBy(groupField, field, having);
    }

    @Override
    public <K, N extends Number> Map<K, Double> sumBy(Field<K> groupField, Field<N> field) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.sumBy(groupField, field);
    }

    @Override
    public <K, N extends Number> Map<K, Double> sumBy(Field<K> groupField, Field<N> field, Condition having)
            throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.sumBy(groupField, field, having);
    }

    @Override
    public <K, N extends Number> Map<K, Double> avgBy(Field<K> groupField, Field<N> field) throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.avgBy(groupField, field);
    }

    @Override
    public <K, N extends Number> Map<K, Double> avgBy(Field<K> groupField, Field<N> field, Condition having)
            throws SQLException {
        LOGGER.log(level, "Aggregating through a decorated select step");
        return origin.avgBy(groupField, field, having);
    }

    @Override
    public <V> SubQuery<V> subQuery(Field<V> field) {
        return origin.subQuery(field);
    }
}
