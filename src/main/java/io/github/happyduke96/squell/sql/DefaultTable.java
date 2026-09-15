package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

/// A `Table` with a primary key; implemented automatically for entities with an `@Id` field. An
/// entity with no `@Id` (a join table, a composite key — squell doesn't model those) gets plain
/// `Table<T>` instead, with no `id()`/`byId()`.
public interface DefaultTable<T, ID> extends Table<T> {

    Field<ID> id();

    default Condition byId(ID value) {
        return id().eq(value);
    }
}
