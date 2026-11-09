package io.github.happyduke96.squell.converter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Converts {@code ZonedDateTime} for binding.
 *
 * <p>No tested dialect stores a zone id, only the instant, so every value is read back at
 * {@code readZone} ({@code UTC} by default). Store the original zone in a separate column if you
 * need it.
 */
public final class ZonedDateTimeConverter implements Converter<ZonedDateTime, Object> {

    private final ZoneId readZone;

    public ZonedDateTimeConverter() {
        this(ZoneId.of("UTC"));
    }

    public ZonedDateTimeConverter(ZoneId readZone) {
        this.readZone = readZone;
    }

    @Override
    public Object toSql(ZonedDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    @Override
    public ZonedDateTime fromSql(Object sqlValue) {
        return switch (sqlValue) {
            case null -> null;
            case Timestamp timestamp -> timestamp.toInstant().atZone(readZone);
            case LocalDateTime localDateTime -> localDateTime.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .atZone(readZone);
            default -> throw new IllegalArgumentException(
                    "Expected Timestamp or LocalDateTime, got " + sqlValue.getClass() + ".");
        };
    }
}
