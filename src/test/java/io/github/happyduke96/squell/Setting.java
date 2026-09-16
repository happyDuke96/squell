package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

/// A nullable-column `UpdateStep.set(field, null)` fixture — `prefix` has no NOT NULL constraint.
@Entity("settings")
public interface Setting {

    @Id
    UUID id();

    String prefix();
}
