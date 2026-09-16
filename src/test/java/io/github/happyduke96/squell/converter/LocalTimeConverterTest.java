package io.github.happyduke96.squell.converter;

import org.testng.annotations.Test;

import java.sql.Time;
import java.time.LocalTime;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class LocalTimeConverterTest {

    private final LocalTimeConverter converter = new LocalTimeConverter();

    @Test
    public void roundTripsThroughAJdbcTime() {
        LocalTime localTime = LocalTime.of(10, 30, 15);

        assertEquals(converter.toSql(localTime), Time.valueOf(localTime));
        assertEquals(converter.fromSql(Time.valueOf(localTime)), localTime);
    }

    @Test
    public void subSecondPrecisionIsLostBecauseJdbcTimeItselfHasNone() {
        LocalTime withNanos = LocalTime.of(10, 30, 15, 123_000_000);

        LocalTime roundTripped = converter.fromSql(converter.toSql(withNanos));

        assertEquals(roundTripped, withNanos.withNano(0));
    }

    @Test
    public void roundTripsArbitraryLocalTimesOnceTruncatedToWholeSeconds() {
        Random random = new Random(20240115);
        for (int i = 0; i < 200; i++) {
            LocalTime time = LocalTime.ofSecondOfDay(random.nextInt(86_400)).withNano(random.nextInt(1_000_000_000));

            LocalTime roundTripped = converter.fromSql(converter.toSql(time));

            assertEquals(roundTripped, time.withNano(0));
        }
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
