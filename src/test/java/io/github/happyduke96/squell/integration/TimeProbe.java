package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.converter.LocalDateConverter;
import io.github.happyduke96.squell.converter.LocalDateTimeConverter;
import io.github.happyduke96.squell.converter.LocalTimeConverter;
import io.github.happyduke96.squell.converter.OffsetDateTimeConverter;
import io.github.happyduke96.squell.converter.ZonedDateTimeConverter;
import io.github.happyduke96.squell.sql.annotation.Convert;
import io.github.happyduke96.squell.sql.annotation.Entity;
import io.github.happyduke96.squell.sql.annotation.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

/// Exercises every shipped `java.time` converter — a hint-less `ResultSet#getObject` returns a
/// different legacy type per dialect for each of these, confirmed empirically; see each
/// converter's javadoc for the exact mapping.
@Entity("time_probe")
public interface TimeProbe {

    @Id
    UUID id();

    @Convert(LocalDateConverter.class)
    LocalDate localDate();

    @Convert(LocalDateTimeConverter.class)
    LocalDateTime localDateTime();

    @Convert(LocalTimeConverter.class)
    LocalTime wallClockTime();

    @Convert(OffsetDateTimeConverter.class)
    OffsetDateTime offsetDateTime();

    @Convert(ZonedDateTimeConverter.class)
    ZonedDateTime zonedDateTime();
}
