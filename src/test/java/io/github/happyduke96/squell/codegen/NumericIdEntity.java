package io.github.happyduke96.squell.codegen;

import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

@Entity("numeric_id_entities")
public interface NumericIdEntity {

    @Id
    long id();

    String label();
}
