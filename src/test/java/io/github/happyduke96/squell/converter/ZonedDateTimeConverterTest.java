package io.github.happyduke96.squell.converter;

import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.expectThrows;

public class ZonedDateTimeConverterTest {

    @Test
    public void toSqlWritesAJdbcTimestampAtTheInstant() {
        ZonedDateTime value = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Tashkent"));

        assertEquals(new ZonedDateTimeConverter().toSql(value), Timestamp.from(value.toInstant()));
    }

    @Test
    public void noArgConstructorReadsBackAtUtcRegardlessOfTheZoneWritten() {
        ZonedDateTimeConverter converter = new ZonedDateTimeConverter();
        ZonedDateTime original = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneId.of("Asia/Tashkent"));

        ZonedDateTime found = converter.fromSql(Timestamp.from(original.toInstant()));

        assertEquals(found.getZone(), ZoneId.of("UTC"));
        assertEquals(found.toInstant(), original.toInstant());
    }

    @Test
    public void explicitReadZoneIsUsedInsteadOfUtc() {
        ZoneId tashkent = ZoneId.of("Asia/Tashkent");
        ZonedDateTimeConverter converter = new ZonedDateTimeConverter(tashkent);
        ZonedDateTime original = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        ZonedDateTime found = converter.fromSql(Timestamp.from(original.toInstant()));

        assertEquals(found.getZone(), tashkent);
        // Same instant, different label — the zone changes what's displayed, not what happened.
        assertEquals(found.toInstant(), original.toInstant());
    }

    @Test
    public void twoConvertersWithDifferentReadZonesDisagreeOnTheWallClockButAgreeOnTheInstant() {
        Timestamp stored = Timestamp.from(ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC).toInstant());

        ZonedDateTime asUtc = new ZonedDateTimeConverter(ZoneOffset.UTC).fromSql(stored);
        ZonedDateTime asTashkent = new ZonedDateTimeConverter(ZoneId.of("Asia/Tashkent")).fromSql(stored);

        assertEquals(asUtc.toInstant(), asTashkent.toInstant());
        assertEquals(asUtc.getHour(), 10);
        assertEquals(asTashkent.getHour(), 15);
    }

    @Test
    public void fromSqlReadsALocalDateTimeAsReportedByMySqlUsingTheReadZone() {
        ZoneId tashkent = ZoneId.of("Asia/Tashkent");
        ZonedDateTimeConverter converter = new ZonedDateTimeConverter(tashkent);
        LocalDateTime wallClock = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        var expectedInstant = wallClock.atZone(ZoneId.systemDefault()).toInstant();

        ZonedDateTime found = converter.fromSql(wallClock);

        assertEquals(found.getZone(), tashkent);
        assertEquals(found.toInstant(), expectedInstant);
    }

    @Test
    public void roundTripsArbitraryInstantsAtTheConfiguredReadZoneRegardlessOfTheZoneWritten() {
        Random random = new Random(20240115);
        for (int i = 0; i < 200; i++) {
            Instant instant = Instant.ofEpochSecond(random.nextLong(-2_000_000_000L, 4_000_000_000L),
                    random.nextInt(1_000_000_000));
            ZoneOffset writeZone = ZoneOffset.ofTotalSeconds(random.nextInt(-64_800, 64_801));
            ZoneOffset readZone = ZoneOffset.ofTotalSeconds(random.nextInt(-64_800, 64_801));
            ZonedDateTimeConverter converter = new ZonedDateTimeConverter(readZone);

            ZonedDateTime found = converter.fromSql(converter.toSql(instant.atZone(writeZone)));

            assertEquals(found.toInstant(), instant);
            assertEquals(found.getZone(), readZone);
        }
    }

    @Test
    public void fromSqlRejectsAnUnrecognizedSqlType() {
        expectThrows(IllegalArgumentException.class,
                () -> new ZonedDateTimeConverter().fromSql("2024-01-15T10:30:00Z"));
    }

    @Test
    public void passesNullThroughUnchanged() {
        ZonedDateTimeConverter converter = new ZonedDateTimeConverter();

        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
