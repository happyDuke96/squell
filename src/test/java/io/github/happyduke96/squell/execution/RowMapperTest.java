package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.Author;
import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.CommentTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.sql.RowMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/// Exercises the `RowMapper` examples in the README: a lambda, a reusable class, and a custom
/// mapper composed from a generated `Table`.
public class RowMapperTest {

    private record PostStats(UUID id, String title, long comments) {
        static final RowMapper<PostStats> MAPPER = row -> new PostStats(
                row.getObject("id", UUID.class),
                row.getString("title"),
                row.getLong("comments"));
    }

    private record PostWithAuthor(Post post, String authorName) {
    }

    private PostgresClient client;
    private PostTable posts;
    private UUID postId;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        posts = new PostTable();
        CommentTable comments = new CommentTable();
        AuthorTable authors = new AuthorTable();
        UUID authorId = UUID.randomUUID();
        postId = UUID.randomUUID();

        client.insert(authors)
                .values(authors.create(authorId, "Ada Lovelace"))
                .execute();
        client.insert(posts)
                .values(posts.create(postId, authorId, "Notes", Post.Status.PUBLISHED))
                .execute();
        client.insert(comments)
                .values(comments.create(postId, "tech", 1))
                .execute();
        client.insert(comments)
                .values(comments.create(postId, "tech", 2))
                .execute();
    }

    @Test
    public void aLambdaMapsAnyProjection() throws SQLException {
        List<String> titles = client.sql("SELECT title FROM posts")
                .result(row -> row.getString("title"));

        assertEquals(titles, List.of("Notes"));
    }

    @Test
    public void aReusableMapperClassMapsAnAggregate() throws SQLException {
        List<PostStats> stats = client.sql("""
                        SELECT p.id, p.title, count(c.id) AS comments
                        FROM posts p LEFT JOIN comments c ON c.postId = p.id
                        GROUP BY p.id, p.title
                        """)
                .result(PostStats.MAPPER);

        assertEquals(stats, List.of(new PostStats(postId, "Notes", 2)));
    }

    @Test
    public void aCustomMapperCanReuseTheReadyMadeTableMapper() throws SQLException {
        RowMapper<PostWithAuthor> withAuthor = row -> new PostWithAuthor(
                posts.map(row),
                row.getString("author_name"));

        Optional<PostWithAuthor> found = client.sql("""
                        SELECT p.*, a.fullName AS author_name
                        FROM posts p JOIN authors a ON a.id = p.authorId
                        WHERE p.id = ?
                        """, postId)
                .resultOne(withAuthor);

        assertEquals(found.orElseThrow().post().title(), "Notes");
        assertEquals(found.orElseThrow().authorName(), "Ada Lovelace");
    }

    @Test
    public void resultOneIsEmptyForNoRowsAndRejectsMoreThanOne() throws SQLException {
        Optional<String> none = client.sql("SELECT title FROM posts WHERE title = ?", "missing")
                .resultOne(row -> row.getString("title"));

        assertTrue(none.isEmpty());

        expectThrows(IllegalStateException.class, () -> client.sql("SELECT c.id FROM comments c")
                .resultOne(row -> row.getObject("id", UUID.class)));
    }

    @Test
    public void aJoinTakesACustomMapperToo() throws SQLException {
        AuthorTable authors = new AuthorTable();
        var a = authors.as("a");
        var p = posts.as("p");

        List<String> names = client.select(a)
                .join(p)
                .on(a.field(authors.id()).eqField(p.field(posts.authorId())))
                .fetch(row -> row.getString(2) + ":" + row.getString(5));

        assertEquals(names.size(), 1);
        assertTrue(names.getFirst().startsWith("Ada Lovelace:"));
    }
}
