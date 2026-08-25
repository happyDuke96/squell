package io.github.happyduke96.squell.converter.mysql;

import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class SetConverterTest {

    private final SetConverter converter = new SetConverter();

    @Test
    public void roundTripsThroughCommaSeparatedText() {
        Set<String> original = new LinkedHashSet<>(Set.of("a", "b", "c"));

        String sqlValue = converter.toSql(original);
        Set<String> roundTripped = converter.fromSql(sqlValue);

        assertEquals(roundTripped, original);
    }

    @Test
    public void emptyOrNullSqlValueReadsAsAnEmptySet() {
        assertEquals(converter.fromSql(""), Set.of());
        assertEquals(converter.fromSql(null), Set.of());
    }

    @Test
    public void toSqlPassesNullThroughUnchanged() {
        assertNull(converter.toSql(null));
    }
}
