package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.sql.AliasedTable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/// Exercises `whereIf` on the step family and `Table.as`.
public class WhereIfAndAsTest {

    private PostgresClient client;
    private PostTable posts;
    private UUID authorId;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        posts = new PostTable();

        authorId = UUID.randomUUID();
        client.insert(new AuthorTable()).values(new AuthorTable().create(authorId, "Ada Lovelace")).execute();

        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "kept", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "dropped", Post.Status.PUBLISHED))
                .execute();
    }

    @Test
    public void selectStepWhereIfSkipsTheConditionWhenTestIsFalse() throws SQLException {
        SelectStep<Post> step = client.select(posts);

        assertSame(step.whereIf(false, () -> posts.status().eq(Post.Status.DRAFT)), step);
        assertEquals(step.fetch().size(), 2);
    }

    @Test
    public void selectStepWhereIfAppliesTheConditionWhenTestIsTrue() throws SQLException {
        List<Post> results = client.select(posts)
                .whereIf(true, () -> posts.status().eq(Post.Status.DRAFT))
                .fetch();

        assertEquals(results.size(), 1);
        assertEquals(results.getFirst().title(), "kept");
    }

    @Test
    public void selectStepWhereIfDoesNotEvaluateTheSupplierWhenTestIsFalse() throws SQLException {
        SelectStep<Post> unused = client.select(posts)
                .whereIf(false, () -> {
                    throw new AssertionError("Supplier must not be evaluated when test is false.");
                });

        assertEquals(unused.fetch().size(), 2);
    }

    @Test
    public void updateStepWhereIfSkipsTheConditionWhenTestIsFalse() throws SQLException {
        UpdateStep<Post> step = client.update(posts).set(posts.status(), Post.Status.PUBLISHED);
        int updated = step.whereIf(false, () -> posts.title().eq("kept")).execute();

        assertEquals(updated, 2);
    }

    @Test
    public void deleteStepWhereIfAppliesTheConditionWhenTestIsTrue() throws SQLException {
        int deleted = client.delete(posts).whereIf(true, () -> posts.title().eq("kept")).execute();

        assertEquals(deleted, 1);
        assertEquals(client.select(posts).fetch().size(), 1);
    }

    @Test
    public void joinStepWhereIfSkipsTheConditionWhenTestIsFalse() throws SQLException {
        AliasedTable<Post> a = posts.as("a");
        JoinStep step = client.select(a).whereIf(false, () -> a.field(posts.title()).eq("kept"));

        List<Post> results = step.fetch(a);
        assertEquals(results.size(), 2);
    }

    @Test
    public void tableAsBuildsTheSameAliasedTableAsTheExplicitConstructor() {
        AliasedTable<Post> viaAs = posts.as("p");
        AliasedTable<Post> viaConstructor = new AliasedTable<>(posts, "p");

        assertEquals(viaAs.alias(), viaConstructor.alias());
        assertEquals(viaAs.tableName(), viaConstructor.tableName());
    }
}
