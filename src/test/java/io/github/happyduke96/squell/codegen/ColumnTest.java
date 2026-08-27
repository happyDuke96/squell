package io.github.happyduke96.squell.codegen;

import io.github.happyduke96.squell.Tag;
import io.github.happyduke96.squell.TagTable;
import io.github.happyduke96.squell.execution.PostgresClient;
import org.h2.jdbcx.JdbcDataSource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/// Exercises `@Column`.
public class ColumnTest {

    private PostgresClient client;
    private TagTable tags;

    @BeforeMethod
    public void setUp() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:squell_column_test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE tags (
                        id UUID PRIMARY KEY,
                        tag_label VARCHAR NOT NULL
                    )
                    """);
        }

        client = new PostgresClient(dataSource);
        tags = new TagTable();
    }

    @Test
    public void fieldNameMatchesTheColumnAnnotationNotTheAccessor() {
        assertEquals(tags.label().name(), "tag_label");
    }

    @Test
    public void insertAndFetchRoundTripThroughTheRenamedColumn() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(tags).values(tags.create(id, "sql")).execute();

        Tag found = client.select(tags).where(tags.id().eq(id)).fetch().getFirst();

        assertEquals(found.label(), "sql");
    }
}
