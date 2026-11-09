package io.github.happyduke96.squell.converter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Converts {@code OffsetDateTime} for binding.
 *
 * <p>No tested dialect preserves the offset, only the instant, so every value is read back at
 * {@code readOffset} ({@code UTC} by default). Store the original offset in a separate column if
 * you need it.
 */
public final class OffsetDateTimeConverter implements Converter<OffsetDateTime, Object> {

    private final ZoneOffset readOffset;

    public OffsetDateTimeConverter() {
        this(ZoneOffset.UTC);
    }

    public OffsetDateTimeConverter(ZoneOffset readOffset) {
        this.readOffset = readOffset;
    }

    @Override
    public Object toSql(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    @Override
    public OffsetDateTime fromSql(Object sqlValue) {
        return switch (sqlValue) {
            case null -> null;
            case Timestamp timestamp -> timestamp.toInstant().atOffset(readOffset);
            case LocalDateTime localDateTime -> localDateTime.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .atOffset(readOffset);
            default -> throw new IllegalArgumentException(
                    "Expected Timestamp or LocalDateTime, got " + sqlValue.getClass() + ".");
        };
    }
}
