package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

import java.time.Instant;

/**
 * Opt-in for a table whose entity tracks {@code createdAt} and {@code updatedAt}.
 *
 * <p>Not generated: implement it by hand on a generated table that has both columns.
 *
 * @param <T> the entity type
 * @param <ID> the primary key type
 */
public interface AuditableTable<T, ID> extends DefaultTable<T, ID> {

    Field<Instant> createdAt();

    Field<Instant> updatedAt();

    default Condition createdAfter(Instant moment) {
        return createdAt().gt(moment);
    }

    default Condition updatedAfter(Instant moment) {
        return updatedAt().gt(moment);
    }
}
