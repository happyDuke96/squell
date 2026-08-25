package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

/// A CRUD/`RETURNING` fixture for real SQLite. `id` needs `@Convert`: `sqlite-jdbc` reads a
/// `TEXT` column back as a `String`, not a `UUID` — confirmed empirically, not assumed.
@Entity("notes")
public interface SqliteNote {

    @Id
    @Convert
    UUID id();

    String note();
}
