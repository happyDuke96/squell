package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;
import io.github.happyduke96.squell.sql.annotation.Version;

import java.util.UUID;

@Entity("documents")
public interface Document {

    @Id
    UUID id();

    @Version
    int version();

    String title();
}
