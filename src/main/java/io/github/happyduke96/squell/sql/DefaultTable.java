package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

/**
 * A {@link Table} with a primary key, implemented automatically for entities with an {@code @Id}
 * field.
 *
 * <p>Entities without one get a plain {@code Table} with no {@code id()} or {@code byId()}.
 *
 * @param <T> the entity type
 * @param <ID> the primary key type
 */
public interface DefaultTable<T, ID> extends Table<T> {

    Field<ID> id();

    default Condition byId(ID value) {
        return id().eq(value);
    }
}
