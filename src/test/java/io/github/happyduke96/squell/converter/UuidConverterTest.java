package io.github.happyduke96.squell.converter;

import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class UuidConverterTest {

    private final UuidConverter converter = new UuidConverter();

    @Test
    public void roundTripsAUuidThroughItsStringForm() {
        UUID id = UUID.randomUUID();

        assertEquals(converter.toSql(id), id.toString());
        assertEquals(converter.fromSql(id.toString()), id);
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
