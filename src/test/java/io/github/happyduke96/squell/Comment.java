package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.GeneratedValue;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.Optional;
import java.util.UUID;

@Entity("comments")
public interface Comment {

    @Id
    @GeneratedValue
    Optional<UUID> id();

    UUID postId();

    String category();

    int upvotes();
}
