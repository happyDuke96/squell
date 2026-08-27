package io.github.happyduke96.squell.converter.mysql;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class VectorConverterTest {

    private final VectorConverter converter = new VectorConverter();

    @Test
    public void roundTripsThroughLittleEndianBytes() {
        float[] original = {1f, 2f, 3f};

        byte[] sqlValue = converter.toSql(original);

        assertEquals(sqlValue.length, 3 * Float.BYTES);
        assertEquals(converter.fromSql(sqlValue), original);
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
