package io.github.happyduke96.squell.converter.postgres;

import org.postgresql.util.PGobject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/// Same no-real-Postgres caveat as [JsonbConverterTest] — `PGobject`-level (de)serialization only.
public class IntRangeConverterTest {

    private final IntRangeConverter converter = new IntRangeConverter();

    @Test
    public void toSqlFormatsAsAnInt4RangeTypedPGobject() {
        PGobject sqlValue = converter.toSql(new Range<>(1, 10));

        assertEquals(sqlValue.getType(), "int4range");
        assertEquals(sqlValue.getValue(), "[1,10)");
    }

    @Test
    public void fromSqlParsesTheRangeLiteralBack() throws Exception {
        PGobject sqlValue = new PGobject();
        sqlValue.setType("int4range");
        sqlValue.setValue("[1,10)");

        assertEquals(converter.fromSql(sqlValue), new Range<>(1, 10));
    }

    @Test
    public void handlesAnUnboundedSide() throws Exception {
        PGobject sqlValue = converter.toSql(new Range<>(null, 10));

        assertEquals(sqlValue.getValue(), "[,10)");
        assertEquals(converter.fromSql(sqlValue), new Range<>(null, 10));
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
