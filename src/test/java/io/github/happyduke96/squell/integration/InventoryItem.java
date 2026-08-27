package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.sql.annotation.Column;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

/// A `RETURNING`/`ON CONFLICT` fixture — `sku` is the conflict target, `quantity` is what an
/// upsert actually changes.
@Entity("inventory")
public interface InventoryItem {

    @Id
    UUID id();

    @Column(unique = true)
    String sku();

    int quantity();
}
