package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

/// A `Table` with a primary key; implemented automatically for entities with an `@Id` field.
public interface DefaultTable<T, ID> extends Table<T> {

    Field<ID> id();

    default Condition byId(ID value) {
        return id().eq(value);
    }
}
