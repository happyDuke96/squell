package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

@Entity("posts")
public interface Post {

    @Id
    UUID id();

    UUID authorId();

    String title();

    Status status();

    enum Status {
        DRAFT, PUBLISHED
    }
}
