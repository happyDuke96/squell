package io.github.happyduke96.squell.codegen;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class NumericIdEntityTest {

    @Test
    public void createRejectsANegativeId() {
        NumericIdEntityTable table = new NumericIdEntityTable();

        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> table.create(-1, "example"));

        assertTrue(thrown.getMessage().contains("[id]"));
    }

    @Test
    public void createAcceptsANonNegativeId() {
        NumericIdEntityTable table = new NumericIdEntityTable();

        NumericIdEntity entity = table.create(0, "example");

        assertTrue(entity.id() == 0);
    }
}
