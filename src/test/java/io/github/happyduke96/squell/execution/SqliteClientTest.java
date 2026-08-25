package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/// Exercises `SqliteClient`.
public class SqliteClientTest {

    private SqliteClient client;
    private PostTable posts;
    private UUID authorId;

    @BeforeMethod
    public void setUp() throws SQLException {
        TestDatabase database = new TestDatabase();
        client = database.sqliteClient();
        posts = new PostTable();

        authorId = UUID.randomUUID();
        database.postgresClient().insert(new AuthorTable())
                .values(new AuthorTable().create(authorId, "Ada Lovelace")).execute();
    }

    @Test
    public void insertUpdateAndDeleteRoundTripThroughTheDelegatedEngine() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "sqlite-post", Post.Status.DRAFT)).execute();

        List<Post> found = client.select(posts).where(posts.id().eq(id)).fetch();
        assertEquals(found.size(), 1);

        int updated = client.update(posts).set(posts.status(), Post.Status.PUBLISHED)
                .where(posts.id().eq(id)).execute();
        assertEquals(updated, 1);

        int deleted = client.delete(posts).where(posts.id().eq(id)).execute();
        assertEquals(deleted, 1);
        assertTrue(client.select(posts).fetch().isEmpty());
    }
}
