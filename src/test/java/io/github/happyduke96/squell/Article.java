package io.github.happyduke96.squell;

import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.List;
import java.util.UUID;

@Entity("articles")
public interface Article {

    @Id
    UUID id();

    @Convert
    List<String> labels();
}
