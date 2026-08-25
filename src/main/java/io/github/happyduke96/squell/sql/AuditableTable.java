package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Condition;
import io.github.happyduke96.squell.condition.Field;

import java.time.Instant;

/// Opt-in for a `Table` whose entity tracks `createdAt`/`updatedAt`; not generated automatically —
/// implement it by hand on a generated `Table` that has both columns.
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
