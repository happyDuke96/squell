package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.converter.InstantConverter;
import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.time.Instant;
import java.util.UUID;

/// An `Instant` binding fixture — a bare `Instant` isn't a JDBC parameter type pgjdbc
/// recognizes, so `occurredAt` routes through `InstantConverter`.
@Entity("events")
public interface Event {

    @Id
    UUID id();

    @Convert(InstantConverter.class)
    Instant occurredAt();
}
