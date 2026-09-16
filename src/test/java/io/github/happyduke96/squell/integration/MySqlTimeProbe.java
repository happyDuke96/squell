package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.converter.LocalDateConverter;
import io.github.happyduke96.squell.converter.LocalDateTimeConverter;
import io.github.happyduke96.squell.converter.LocalTimeConverter;
import io.github.happyduke96.squell.converter.OffsetDateTimeConverter;
import io.github.happyduke96.squell.converter.UuidConverter;
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

/// Same shape as `TimeProbe`, for MySQL — `id` needs `@Convert` the same way every other MySQL
/// UUID fixture in this suite does.
@Entity("time_probe")
public interface MySqlTimeProbe {

    @Id
    @Convert(UuidConverter.class)
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
