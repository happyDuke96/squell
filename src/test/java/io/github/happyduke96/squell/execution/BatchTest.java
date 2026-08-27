package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.Author;
import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.support.Batch;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/// Exercises `Batch`.
public class BatchTest {

    private PostgresClient client;
    private AuthorTable authors;
    private PostTable posts;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        authors = new AuthorTable();
        posts = new PostTable();
    }

    @Test
    public void groupsPostsUnderTheirAuthorWithoutAJoin() throws SQLException {
        Author withPosts = authors.create(UUID.randomUUID(), "Ada Lovelace");
        Author withNoPosts = authors.create(UUID.randomUUID(), "Alan Turing");
        client.insert(authors).values(withPosts).execute();
        client.insert(authors).values(withNoPosts).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), withPosts.id(),
                "Notes on the Analytical Engine", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), withPosts.id(),
                "Sketch of the Analytical Engine", Post.Status.PUBLISHED)).execute();

        List<Author> allAuthors = client.select(authors).fetch();
        List<Post> allPosts = client.select(posts).fetch();

        Map<Author, List<Post>> grouped = new Batch<>(allAuthors, Author::id, allPosts, Post::authorId).groupBy();

        assertEquals(grouped.get(withPosts).size(), 2);
        assertTrue(grouped.get(withNoPosts).isEmpty());
    }
}
