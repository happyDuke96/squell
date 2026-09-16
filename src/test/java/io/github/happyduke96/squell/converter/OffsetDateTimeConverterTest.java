package io.github.happyduke96.squell.converter;

import org.testng.annotations.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.expectThrows;

public class OffsetDateTimeConverterTest {

    @Test
    public void toSqlWritesAJdbcTimestampAtTheInstant() {
        OffsetDateTime value = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.of("+05:00"));

        assertEquals(new OffsetDateTimeConverter().toSql(value), Timestamp.from(value.toInstant()));
    }

    @Test
    public void noArgConstructorReadsBackAtUtcRegardlessOfTheOffsetWritten() {
        OffsetDateTimeConverter converter = new OffsetDateTimeConverter();
        OffsetDateTime original = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.of("+05:00"));

        OffsetDateTime found = converter.fromSql(Timestamp.from(original.toInstant()));

        assertEquals(found.getOffset(), ZoneOffset.UTC);
        assertEquals(found.toInstant(), original.toInstant());
    }

    @Test
    public void explicitReadOffsetIsUsedInsteadOfUtc() {
        OffsetDateTimeConverter converter = new OffsetDateTimeConverter(ZoneOffset.of("+05:00"));
        OffsetDateTime original = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        OffsetDateTime found = converter.fromSql(Timestamp.from(original.toInstant()));

        assertEquals(found.getOffset(), ZoneOffset.of("+05:00"));
        // Same instant, different label — the offset changes what's displayed, not what happened.
        assertEquals(found.toInstant(), original.toInstant());
    }

    @Test
    public void twoConvertersWithDifferentReadOffsetsDisagreeOnTheDisplayedOffsetButAgreeOnTheInstant() {
        Timestamp stored = Timestamp.from(OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC).toInstant());

        OffsetDateTime asUtc = new OffsetDateTimeConverter(ZoneOffset.UTC).fromSql(stored);
        OffsetDateTime asPlusFive = new OffsetDateTimeConverter(ZoneOffset.of("+05:00")).fromSql(stored);

        assertEquals(asUtc.toInstant(), asPlusFive.toInstant());
        assertEquals(asUtc.getHour(), 10);
        assertEquals(asPlusFive.getHour(), 15);
    }

    @Test
    public void fromSqlReadsALocalDateTimeAsReportedByMySqlUsingTheReadOffset() {
        OffsetDateTimeConverter converter = new OffsetDateTimeConverter(ZoneOffset.of("+05:00"));
        LocalDateTime wallClock = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        var expectedInstant = wallClock.atZone(ZoneId.systemDefault()).toInstant();

        OffsetDateTime found = converter.fromSql(wallClock);

        assertEquals(found.getOffset(), ZoneOffset.of("+05:00"));
        assertEquals(found.toInstant(), expectedInstant);
    }

    @Test
    public void roundTripsArbitraryInstantsAtTheConfiguredReadOffsetRegardlessOfTheOffsetWritten() {
        Random random = new Random(20240115);
        for (int i = 0; i < 200; i++) {
            Instant instant = Instant.ofEpochSecond(random.nextLong(-2_000_000_000L, 4_000_000_000L),
                    random.nextInt(1_000_000_000));
            ZoneOffset writeOffset = ZoneOffset.ofTotalSeconds(random.nextInt(-64_800, 64_801));
            ZoneOffset readOffset = ZoneOffset.ofTotalSeconds(random.nextInt(-64_800, 64_801));
            OffsetDateTimeConverter converter = new OffsetDateTimeConverter(readOffset);

            OffsetDateTime found = converter.fromSql(converter.toSql(instant.atOffset(writeOffset)));

            assertEquals(found.toInstant(), instant);
            assertEquals(found.getOffset(), readOffset);
        }
    }

    @Test
    public void fromSqlRejectsAnUnrecognizedSqlType() {
        expectThrows(IllegalArgumentException.class,
                () -> new OffsetDateTimeConverter().fromSql("2024-01-15T10:30:00Z"));
    }

    @Test
    public void passesNullThroughUnchanged() {
        OffsetDateTimeConverter converter = new OffsetDateTimeConverter();

        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
