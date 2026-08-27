package io.github.happyduke96.squell.codegen;

import io.github.happyduke96.squell.converter.UuidConverter;
import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.util.UUID;

@Entity("named_converter_entities")
public interface NamedConverterEntity {

    @Id
    @Convert(UuidConverter.class)
    UUID id();

    String label();
}
