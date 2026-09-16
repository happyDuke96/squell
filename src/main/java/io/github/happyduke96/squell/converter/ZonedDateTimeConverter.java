package io.github.happyduke96.squell.converter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/// Converts `ZonedDateTime` for binding — same missing-SQL-type issue as `Instant`. No dialect
/// tested stores a zone id, only an instant — every value comes back at `readZone` (`UTC` by
/// default). Store the original zone yourself in a separate column if you need it.
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
