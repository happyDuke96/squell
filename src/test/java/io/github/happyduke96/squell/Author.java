package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

@Entity("authors")
public interface Author {

    @Id
    UUID id();

    String fullName();
}
