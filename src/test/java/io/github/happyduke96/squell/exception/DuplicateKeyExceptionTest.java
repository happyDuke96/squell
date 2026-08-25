package io.github.happyduke96.squell.exception;

import io.github.happyduke96.squell.TagTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.execution.PostgresClient;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.UUID;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/// Exercises `DataAccessException.translate` against a real unique-constraint violation.
public class DuplicateKeyExceptionTest {

    private PostgresClient client;
    private TagTable tags;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        tags = new TagTable();
        client.insert(tags).values(tags.create(UUID.randomUUID(), "sql")).execute();
    }

    @Test
    public void insertingADuplicateLabelThrowsDuplicateKeyException() {
        DuplicateKeyException thrown = expectThrows(DuplicateKeyException.class,
                () -> client.insert(tags).values(tags.create(UUID.randomUUID(), "sql")).execute());

        assertTrue(thrown instanceof DataAccessException);
    }
}
