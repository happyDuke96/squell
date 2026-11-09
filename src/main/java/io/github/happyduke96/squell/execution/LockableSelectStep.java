package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

import java.util.function.Supplier;

/**
 * A {@link SelectStep} that supports row locking, for dialects with {@code SELECT ... FOR UPDATE}
 * (not SQLite).
 *
 * @param <T> the entity type
 */
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

    /**
     * Appends {@code FOR UPDATE}: locks the matched rows until the transaction ends.
     *
     * @return this step with the lock clause
     */
    SelectStep<T> forUpdate();

    /**
     * Appends {@code FOR UPDATE SKIP LOCKED}: like {@link #forUpdate()}, but rows locked by another
     * transaction are skipped instead of waited for.
     *
     * @return this step with the lock clause
     */
    SelectStep<T> forUpdateSkipLocked();

    /**
     * Appends {@code FOR UPDATE NOWAIT}: like {@link #forUpdate()}, but fails immediately when a
     * matched row is locked by another transaction.
     *
     * @return this step with the lock clause
     */
    SelectStep<T> forUpdateNoWait();

    /**
     * Appends {@code FOR SHARE}: a shared lock that blocks updates and deletes of the matched rows
     * but allows other shared locks.
     *
     * @return this step with the lock clause
     */
    SelectStep<T> forShare();
}
