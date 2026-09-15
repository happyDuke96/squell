package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

/// A field-to-field `UpdateStep.set()` fixture — `pendingBalance` gets released into `balance`.
@Entity("accounts")
public interface Account {

    @Id
    UUID id();

    int balance();

    int pendingBalance();
}
