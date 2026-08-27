package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.Set;
import java.util.UUID;

/// A fixture exercising the MySQL-only converters against real column types — `VARBINARY` for
/// the vector, and MySQL's native `SET(...)` for the set, not just their in-isolation
/// (de)serialization logic. `id` needs `@Convert` too: MySQL Connector/J has no native mapping for
/// a raw `UUID` bound to `CHAR(36)` — confirmed empirically, not assumed (it Java-serializes the
/// object instead of calling `toString()`).
@Entity("mysql_types")
public interface MySqlTypes {

    @Id
    @Convert
    UUID id();

    @Convert
    float[] embedding();

    @Convert
    Set<String> labels();
}
