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
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

/// Exercises `SelectStep.fetchStream`.
public class FetchStreamTest {

    private PostgresClient client;
    private PostTable posts;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        posts = new PostTable();

        UUID authorId = UUID.randomUUID();
        client.insert(new AuthorTable()).values(new AuthorTable().create(authorId, "Ada Lovelace")).execute();

        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "first", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "second", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "third", Post.Status.DRAFT)).execute();
    }

    @Test
    public void fetchStreamYieldsEveryMatchingRow() throws SQLException {
        List<String> titles;
        try (Stream<Post> stream = client.select(posts).orderBy(posts.title()).fetchStream()) {
            titles = stream.map(Post::title).toList();
        }

        assertEquals(titles, List.of("first", "second", "third"));
    }

    @Test
    public void fetchStreamSupportsShortCircuitingWithoutReadingEveryRow() throws SQLException {
        String first;
        try (Stream<Post> stream = client.select(posts).orderBy(posts.title()).fetchStream()) {
            first = stream.findFirst().map(Post::title).orElseThrow();
        }

        assertEquals(first, "first");
    }
}
