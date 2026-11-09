package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Field;

/// A `Table` with an optimistic-lock counter; implemented automatically for entities with a
/// `@Version` field.
public interface VersionedTable<T, V> extends Table<T> {

    Field<V> version();
}
