package io.github.happyduke96.squell.converter.postgres;

import org.postgresql.util.PGobject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/// Same no-real-Postgres caveat as [JsonbConverterTest] — `PGobject`-level (de)serialization only.
public class VectorConverterTest {

    private final VectorConverter converter = new VectorConverter();

    @Test
    public void toSqlFormatsAsAVectorTypedPGobject() {
        PGobject sqlValue = converter.toSql(new float[] {1f, 2f, 3f});

        assertEquals(sqlValue.getType(), "vector");
        assertEquals(sqlValue.getValue(), "[1.0,2.0,3.0]");
    }

    @Test
    public void fromSqlParsesTheVectorTextBack() throws Exception {
        PGobject sqlValue = new PGobject();
        sqlValue.setType("vector");
        sqlValue.setValue("[1.0,2.0,3.0]");

        assertEquals(converter.fromSql(sqlValue), new float[] {1f, 2f, 3f});
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
