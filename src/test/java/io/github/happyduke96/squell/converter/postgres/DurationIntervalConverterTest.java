package io.github.happyduke96.squell.converter.postgres;

import org.postgresql.util.PGInterval;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

/// `PGInterval` needs no live connection to construct — this only exercises the (de)serialization
/// logic, not a real `interval` column; no Postgres is available in this project (H2-only by scope).
public class DurationIntervalConverterTest {

    private final DurationIntervalConverter converter = new DurationIntervalConverter();

    @Test
    public void roundTripsADurationThroughDaysHoursMinutesSeconds() {
        Duration original = Duration.ofDays(2).plusHours(3).plusMinutes(4).plusSeconds(5);

        PGInterval interval = converter.toSql(original);
        Duration roundTripped = converter.fromSql(interval);

        assertEquals(roundTripped, original);
    }

    @Test
    public void rejectsAnIntervalCarryingYearsOrMonths() {
        PGInterval interval = new PGInterval(1, 0, 0, 0, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> converter.fromSql(interval));
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
