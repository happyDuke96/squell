package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Field;

/**
 * A {@link Table} with an optimistic-lock counter, implemented automatically for entities with a
 * {@code @Version} field.
 *
 * @param <T> the entity type
 * @param <V> the version type
 * @since 0.3.0
 */
public interface VersionedTable<T, V> extends Table<T> {

    Field<V> version();
}
