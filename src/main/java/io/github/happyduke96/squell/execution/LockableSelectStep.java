package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

import java.util.function.Supplier;

/// A `SelectStep` that also supports row locking — only where the dialect actually has
/// `SELECT ... FOR UPDATE` (not `SqliteClient`).
public interface LockableSelectStep<T> extends SelectStep<T> {

    @Override
    LockableSelectStep<T> where(Condition condition);

    @Override
    default LockableSelectStep<T> whereIf(boolean test, Supplier<Condition> condition) {
        return test ? where(condition.get()) : this;
    }

    @Override
    LockableSelectStep<T> orderBy(Field<?> field);

    @Override
    LockableSelectStep<T> orderByDesc(Field<?> field);

    @Override
    LockableSelectStep<T> limit(int limit);

    @Override
    LockableSelectStep<T> offset(int offset);

    @Override
    LockableSelectStep<T> distinct();

    /// `SELECT ... FOR UPDATE` — locks the matched rows until the transaction commits or rolls
    /// back, so no other transaction can update or delete them concurrently.
    SelectStep<T> forUpdate();

    /// `SELECT ... FOR UPDATE SKIP LOCKED` — like `forUpdate`, but rows already locked by
    /// another transaction are silently excluded instead of blocking.
    SelectStep<T> forUpdateSkipLocked();

    /// `SELECT ... FOR UPDATE NOWAIT` — like `forUpdate`, but fails immediately with an error
    /// instead of blocking when a matched row is already locked by another transaction.
    SelectStep<T> forUpdateNoWait();

    /// `SELECT ... FOR SHARE` — a shared read lock: blocks other transactions from updating or
    /// deleting the matched rows, but not from also taking a `FOR SHARE` lock on them.
    SelectStep<T> forShare();
}
