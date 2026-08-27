package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

/// A `boolean` column fixture for real SQLite: there's no native `BOOLEAN` type, so it's stored
/// with `INTEGER` affinity and `getObject` returns an `Integer`, not a `Boolean` — confirmed
/// empirically, not assumed.
@Entity("flags")
public interface SqliteFlag {

    @Id
    @Convert
    UUID id();

    boolean enabled();
}
