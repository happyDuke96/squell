package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.converter.InstantConverter;
import io.github.happyduke96.squell.converter.UuidConverter;
import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.time.Instant;
import java.util.UUID;

/// An `Instant` binding fixture for MySQL — `id` needs `@Convert` the same way every other
/// MySQL UUID fixture in this suite does; `occurredAt` routes through `InstantConverter`.
@Entity("mysql_events")
public interface MySqlEvent {

    @Id
    @Convert(UuidConverter.class)
    UUID id();

    @Convert(InstantConverter.class)
    Instant occurredAt();
}
