package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.converter.postgres.Range;
import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/// A fixture exercising the Postgres-only converters against their real column types —
/// `jsonb`/`text[]`/`interval`/`int4range` — not just the `PGobject`/`PGInterval` (de)serialization
/// logic H2 lets the unit tests reach.
@Entity("postgres_types")
public interface PostgresTypes {

    @Id
    UUID id();

    @Convert
    String payload();

    @Convert
    List<String> tags();

    @Convert
    Duration duration();

    @Convert
    Range<Integer> range();
}
