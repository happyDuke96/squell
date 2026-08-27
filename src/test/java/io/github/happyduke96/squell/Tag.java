package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Column;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

@Entity("tags")
public interface Tag {

    @Id
    UUID id();

    @Column(value = "tag_label", nonNull = true, unique = true)
    String label();
}
