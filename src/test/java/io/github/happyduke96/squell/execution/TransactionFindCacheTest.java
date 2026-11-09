package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.Document;
import io.github.happyduke96.squell.DocumentTable;
import io.github.happyduke96.squell.TestDatabase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/// Exercises `Transaction#findCached`'s per-transaction identity cache, the auto-invalidation
/// `update`/`delete` do on success, and `#forget` for writes that bypass them.
public class TransactionFindCacheTest {

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
    public void secondFindWithinTheSameTransactionReturnsTheCachedInstanceWithoutRequerying() throws SQLException {
        client.transactionWithoutResult(tx -> {
            Document first = tx.findCached(documents, id).orElseThrow();
            Document second = tx.findCached(documents, id).orElseThrow();

            assertSame(second, first);
        });
    }

    @Test
    public void updateThroughTheSameTransactionAutomaticallyForgetsTheCachedValue() throws SQLException {
        client.transactionWithoutResult(tx -> {
            Document cached = tx.findCached(documents, id).orElseThrow();
            assertEquals(cached.title(), "draft");

            tx.update(documents)
                    .set(documents.title(), "updated")
                    .where(documents.id().eq(id))
                    .execute();

            Document afterUpdate = tx.findCached(documents, id).orElseThrow();
            assertEquals(afterUpdate.title(), "updated", "update() should forget this table's cache on success");
        });
    }

    @Test
    public void deleteThroughTheSameTransactionAutomaticallyForgetsTheCachedValue() throws SQLException {
        client.transactionWithoutResult(tx -> {
            tx.findCached(documents, id).orElseThrow();

            tx.delete(documents)
                    .where(documents.id().eq(id))
                    .execute();

            Optional<Document> afterDelete = tx.findCached(documents, id);
            assertTrue(afterDelete.isEmpty(), "delete() should forget this table's cache on success");
        });
    }

    // updateAndReturn()/deleteAndReturn() precise-invalidation is exercised in
    // PostgresIntegrationTest — they use "RETURNING *", which H2 (this class's backing store)
    // doesn't support.

    @Test
    public void aNotFoundResultIsCachedTooEvenAfterARowAppearsBehindItsBack() throws SQLException {
        UUID missingId = UUID.randomUUID();

        client.transactionWithoutResult(tx -> {
            Optional<Document> firstLookup = tx.findCached(documents, missingId);
            assertTrue(firstLookup.isEmpty());

            tx.insert(documents)
                    .values(documents.create(missingId, "snuck in"))
                    .execute();

            Optional<Document> secondLookup = tx.findCached(documents, missingId);
            assertTrue(secondLookup.isEmpty(),
                    "insert() bypasses the auto-invalidation update()/delete() do, so the cached miss stands");
        });
    }

    @Test
    public void forgetFixesStalenessAfterAWriteThatBypassesUpdateAndDelete() throws SQLException {
        UUID missingId = UUID.randomUUID();

        client.transactionWithoutResult(tx -> {
            tx.findCached(documents, missingId);
            tx.insert(documents)
                    .values(documents.create(missingId, "snuck in"))
                    .execute();
            tx.forget(documents, missingId);

            Document afterForget = tx.findCached(documents, missingId).orElseThrow();
            assertEquals(afterForget.title(), "snuck in");
        });
    }
}
