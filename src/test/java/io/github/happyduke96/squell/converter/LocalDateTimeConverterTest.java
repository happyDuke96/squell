package io.github.happyduke96.squell.converter;

import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.expectThrows;

public class LocalDateTimeConverterTest {

    private final LocalDateTimeConverter converter = new LocalDateTimeConverter();

    @Test
    public void toSqlWritesAJdbcTimestamp() {
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        assertEquals(converter.toSql(localDateTime), Timestamp.valueOf(localDateTime));
    }

    @Test
    public void fromSqlReadsATimestampAsReportedByPostgres() {
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        assertEquals(converter.fromSql(Timestamp.valueOf(localDateTime)), localDateTime);
    }

    @Test
    public void fromSqlReadsALocalDateTimeAsReportedByMySqlUnchanged() {
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        assertEquals(converter.fromSql(localDateTime), localDateTime);
    }

    @Test
    public void roundTripsArbitraryLocalDateTimes() {
        Random random = new Random(20240115);
        for (int i = 0; i < 200; i++) {
            LocalDateTime dateTime = LocalDateTime.ofEpochSecond(random.nextLong(-2_000_000_000L, 4_000_000_000L),
                    random.nextInt(1_000_000_000), ZoneOffset.UTC);

            assertEquals(converter.fromSql(converter.toSql(dateTime)), dateTime);
        }
    }

    @Test
    public void fromSqlRejectsAnUnrecognizedSqlType() {
        expectThrows(IllegalArgumentException.class, () -> converter.fromSql("2024-01-15 10:30:00"));
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
