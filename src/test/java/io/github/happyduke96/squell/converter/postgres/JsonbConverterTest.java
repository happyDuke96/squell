package io.github.happyduke96.squell.converter.postgres;

import org.postgresql.util.PGobject;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/// `PGobject` needs no live connection to construct — this only exercises the (de)serialization
/// logic, not a real `jsonb` column; no Postgres is available in this project (H2-only by scope).
public class JsonbConverterTest {

    private final JsonbConverter converter = new JsonbConverter();

    @Test
    public void toSqlWrapsTheJsonTextAsAJsonbTypedPGobject() {
        PGobject sqlValue = converter.toSql("{\"a\":1}");

        assertEquals(sqlValue.getType(), "jsonb");
        assertEquals(sqlValue.getValue(), "{\"a\":1}");
    }

    @Test
    public void fromSqlReadsTheJsonTextBackUnparsed() throws Exception {
        PGobject sqlValue = new PGobject();
        sqlValue.setType("jsonb");
        sqlValue.setValue("{\"a\":1}");

        assertEquals(converter.fromSql(sqlValue), "{\"a\":1}");
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
