package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.Document;
import io.github.happyduke96.squell.DocumentTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.exception.OptimisticLockException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

/// Exercises `@Version` and `UpdateStep#whereVersion`.
public class DocumentTest {

    private PostgresClient client;
    private DocumentTable documents;
    private UUID id;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        documents = new DocumentTable();
        id = UUID.randomUUID();

        client.insert(documents)
                .values(documents.create(id, "draft"))
                .execute();
    }

    @Test
    public void createStartsAtVersionZero() {
        Document draft = documents.create(id, "draft");

        assertEquals(draft.version(), 0);
    }

    @Test
    public void whereVersionSucceedsWhenTheVersionMatchesAndIncrementsIt() throws SQLException {
        int affected = client.update(documents)
                .set(documents.title(), "final")
                .where(documents.id().eq(id))
                .whereVersion(documents.version(), 0)
                .execute();

        assertEquals(affected, 1);

        Document found = client.select(documents)
                .where(documents.id().eq(id))
                .fetch()
                .getFirst();
        assertEquals(found.title(), "final");
        assertEquals(found.version(), 1);
    }

    @Test
    public void whereVersionThrowsInsteadOfSilentlyDoingNothingOnAStaleVersion() throws SQLException {
        client.update(documents)
                .set(documents.title(), "first update")
                .where(documents.id().eq(id))
                .whereVersion(documents.version(), 0)
                .execute();

        expectThrows(OptimisticLockException.class, () -> client.update(documents)
                .set(documents.title(), "conflicting update")
                .where(documents.id().eq(id))
                .whereVersion(documents.version(), 0)
                .execute());
    }

    @Test
    public void whereVersionAloneWithoutASetCallIsRejected() {
        expectThrows(IllegalStateException.class, () -> client.update(documents)
                .where(documents.id().eq(id))
                .whereVersion(documents.version(), 0)
                .execute());
    }
}
