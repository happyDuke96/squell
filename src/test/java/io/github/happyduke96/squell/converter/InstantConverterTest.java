package io.github.happyduke96.squell.converter;

import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.expectThrows;

public class InstantConverterTest {

    private final InstantConverter converter = new InstantConverter();

    @Test
    public void toSqlWritesAJdbcTimestamp() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");

        assertEquals(converter.toSql(instant), Timestamp.from(instant));
    }

    @Test
    public void fromSqlReadsATimestampAsReportedByPostgres() {
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");

        assertEquals(converter.fromSql(Timestamp.from(instant)), instant);
    }

    @Test
    public void fromSqlReadsALocalDateTimeAsReportedByMySql() {
        LocalDateTime wallClock = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Instant expected = wallClock.atZone(ZoneId.systemDefault()).toInstant();

        assertEquals(converter.fromSql(wallClock), expected);
    }

    @Test
    public void roundTripsArbitraryInstants() {
        Random random = new Random(20240115);
        for (int i = 0; i < 200; i++) {
            Instant instant = Instant.ofEpochSecond(random.nextLong(-2_000_000_000L, 4_000_000_000L),
                    random.nextInt(1_000_000_000));

            assertEquals(converter.fromSql(converter.toSql(instant)), instant);
        }
    }

    @Test
    public void fromSqlRejectsAnUnrecognizedSqlType() {
        expectThrows(IllegalArgumentException.class, () -> converter.fromSql("2024-01-15T10:30:00Z"));
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
