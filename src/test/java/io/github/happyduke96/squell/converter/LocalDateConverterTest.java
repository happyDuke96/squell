package io.github.happyduke96.squell.converter;

import org.testng.annotations.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class LocalDateConverterTest {

    private final LocalDateConverter converter = new LocalDateConverter();

    @Test
    public void roundTripsThroughAJdbcDate() {
        LocalDate localDate = LocalDate.of(2024, 1, 15);

        assertEquals(converter.toSql(localDate), Date.valueOf(localDate));
        assertEquals(converter.fromSql(Date.valueOf(localDate)), localDate);
    }

    @Test
    public void roundTripsArbitraryLocalDates() {
        Random random = new Random(20240115);
        for (int i = 0; i < 200; i++) {
            LocalDate date = LocalDate.ofEpochDay(random.nextLong(-25_000, 55_000));

            assertEquals(converter.fromSql(converter.toSql(date)), date);
        }
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
