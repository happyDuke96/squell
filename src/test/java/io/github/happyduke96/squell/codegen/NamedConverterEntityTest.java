package io.github.happyduke96.squell.codegen;

import io.github.happyduke96.squell.converter.UuidConverter;
import io.github.happyduke96.squell.execution.PostgresClient;
import org.h2.jdbcx.JdbcDataSource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/// Exercises the no-arg constructor `EntityProcessor` generates when every `@Convert` field names
/// a converter class, alongside the explicit constructor it already generated.
public class NamedConverterEntityTest {

    private PostgresClient client;

    @BeforeMethod
    public void setUp() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:squell_named_converter_test_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE named_converter_entities (
                        id VARCHAR PRIMARY KEY,
                        label VARCHAR NOT NULL
                    )
                    """);
        }
        client = new PostgresClient(dataSource);
    }

    @Test
    public void noArgConstructorInsertsAndReadsBackTheSameAsTheExplicitOne() throws SQLException {
        NamedConverterEntityTable table = new NamedConverterEntityTable();
        UUID id = UUID.randomUUID();

        client.insert(table)
                .values(table.create(id, "example"))
                .execute();

        NamedConverterEntity found = client.select(table)
                .where(table.id().eq(id))
                .fetchOne()
                .orElseThrow();

        assertEquals(found.id(), id);
        assertEquals(found.label(), "example");
    }

    @Test
    public void explicitConstructorStillWorksAlongsideTheNoArgOne() throws SQLException {
        NamedConverterEntityTable table = new NamedConverterEntityTable(new UuidConverter());
        UUID id = UUID.randomUUID();

        client.insert(table)
                .values(table.create(id, "explicit"))
                .execute();

        NamedConverterEntity found = client.select(table)
                .where(table.id().eq(id))
                .fetchOne()
                .orElseThrow();

        assertEquals(found.label(), "explicit");
    }
}
