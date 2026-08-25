package io.github.happyduke96.squell.codegen;

import io.github.happyduke96.squell.Tables;
import io.github.happyduke96.squell.sql.Table;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;

/// Exercises the `Tables` registry `EntityProcessor` generates for `@GenerateTableRegistry`
/// (see `package-info.java` on `io.github.happyduke96.squell`).
public class TableRegistryTest {

    @Test
    public void allIncludesEntitiesWithNoArgConstructors() {
        List<Table<?>> tables = Tables.all();

        assertTrue(tables.stream().anyMatch(t -> t.name().equals("authors")),
                "Author has no @Convert fields, so it must be in the registry.");
        assertTrue(tables.stream().anyMatch(t -> t.name().equals("named_converter_entities")),
                "NamedConverterEntity names its converter, so it must be in the registry.");
    }

    @Test
    public void allExcludesEntitiesNeedingAnExplicitConverter() {
        List<Table<?>> tables = Tables.all();

        assertTrue(tables.stream().noneMatch(t -> t.name().equals("articles")),
                "Article's @Convert fields don't name a converter, so it can't have a no-arg constructor.");
    }
}
