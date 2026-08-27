package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.exception.DataAccessException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/// Exercises `MySqlClient`.
public class MySqlClientTest {

    private MySqlClient client;
    private PostTable posts;
    private UUID authorId;

    @BeforeMethod
    public void setUp() throws SQLException {
        TestDatabase database = new TestDatabase();
        client = database.mySqlClient();
        posts = new PostTable();

        authorId = UUID.randomUUID();
        database.postgresClient().insert(new AuthorTable())
                .values(new AuthorTable().create(authorId, "Ada Lovelace")).execute();
    }

    @Test
    public void insertUpdateAndDeleteRoundTripThroughTheDelegatedEngine() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "mysql-post", Post.Status.DRAFT)).execute();

        List<Post> found = client.select(posts).where(posts.id().eq(id)).fetch();
        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().status(), Post.Status.DRAFT);

        int updated = client.update(posts).set(posts.status(), Post.Status.PUBLISHED)
                .where(posts.id().eq(id)).execute();
        assertEquals(updated, 1);
        assertEquals(client.select(posts).where(posts.id().eq(id)).fetch().getFirst().status(), Post.Status.PUBLISHED);

        int deleted = client.delete(posts).where(posts.id().eq(id)).execute();
        assertEquals(deleted, 1);
        assertTrue(client.select(posts).where(posts.id().eq(id)).fetch().isEmpty());
    }

    @Test
    public void transactionCommitsAcrossMultipleInserts() throws SQLException {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        client.transactionWithoutResult(tx -> {
            tx.insert(posts).values(posts.create(first, authorId, "tx-1", Post.Status.DRAFT)).execute();
            tx.insert(posts).values(posts.create(second, authorId, "tx-2", Post.Status.DRAFT)).execute();
        });

        assertEquals(client.select(posts).fetch().size(), 2);
    }

    @Test
    public void transactionRollsBackWhenTheWorkThrowsAnError() throws SQLException {
        UUID id = UUID.randomUUID();

        try {
            client.transactionWithoutResult(tx -> {
                tx.insert(posts).values(posts.create(id, authorId, "tx-error", Post.Status.DRAFT)).execute();
                throw new OutOfMemoryError("simulated");
            });
            fail("Expected the Error to propagate");
        } catch (OutOfMemoryError expected) {
            // propagation is the point of this test
        }

        assertTrue(client.select(posts).where(posts.id().eq(id)).fetch().isEmpty());
    }

    @Test
    public void fetchTranslatesASqlExceptionInsteadOfLeakingTheRawOne() {
        Field<String> missingColumn = new Field<>("does_not_exist", String.class);

        try {
            client.select(posts).where(missingColumn.eq("x")).fetch();
            fail("Expected a DataAccessException");
        } catch (DataAccessException expected) {
            // translated, as intended
        } catch (SQLException e) {
            fail("Expected a translated DataAccessException, got a raw SQLException", e);
        }
    }
}
