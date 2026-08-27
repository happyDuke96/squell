package io.github.happyduke96.squell.converter.postgres;

import org.postgresql.util.PGobject;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/// Same H2/no-real-Postgres caveat as [JsonbConverterTest] — only the array-literal (de)serialization
/// logic is exercised here.
public class TextArrayConverterTest {

    private final TextArrayConverter converter = new TextArrayConverter();

    @Test
    public void toSqlFormatsAsATextArrayTypedPGobject() {
        PGobject sqlValue = (PGobject) converter.toSql(List.of("sql", "oop"));

        assertEquals(sqlValue.getType(), "text[]");
        assertEquals(sqlValue.getValue(), "{sql,oop}");
    }

    @Test
    public void fromSqlParsesTheArrayLiteralBack() throws Exception {
        PGobject sqlValue = new PGobject();
        sqlValue.setType("text[]");
        sqlValue.setValue("{sql,oop}");

        assertEquals(converter.fromSql(sqlValue), List.of("sql", "oop"));
    }

    @Test
    public void quotesElementsThatContainSpecialCharacters() throws Exception {
        PGobject sqlValue = (PGobject) converter.toSql(Arrays.asList("has,comma", "has\"quote", null));

        assertEquals(sqlValue.getValue(), "{\"has,comma\",\"has\\\"quote\",NULL}");
        assertEquals(converter.fromSql(sqlValue), Arrays.asList("has,comma", "has\"quote", null));
    }

    @Test
    public void passesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
        assertNull(converter.fromSql(null));
    }
}
