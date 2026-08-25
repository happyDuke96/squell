package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.sql.AliasedTable;
import io.github.happyduke96.squell.sql.RowMapper;
import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface JoinStep {

    PendingJoin join(AliasedTable<?> other);

    PendingJoin leftJoin(AliasedTable<?> other);

    JoinStep where(Condition condition);

    default JoinStep whereIf(boolean test, Supplier<Condition> condition) {
        return test ? where(condition.get()) : this;
    }

    JoinStep orderBy(Field<?> field);

    JoinStep orderByDesc(Field<?> field);

    JoinStep limit(int limit);

    JoinStep offset(int offset);

    JoinStep distinct();

    <R> List<R> fetch(RowMapper<R> mapper) throws SQLException;

    <T> List<T> fetch(AliasedTable<T> only) throws SQLException;

    <R> Optional<R> fetchOne(RowMapper<R> mapper) throws SQLException;

    <T> Optional<T> fetchOne(AliasedTable<T> only) throws SQLException;

    interface PendingJoin {
        JoinStep on(Condition condition);
    }
}
