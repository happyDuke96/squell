package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.Author;
import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.sql.AliasedTable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/// Exercises `JoinStep`.
public class JoinTest {

    private record AuthorPost(String authorName, String postTitle) {
    }

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
    public void joinMatchesRowsAcrossBothTables() throws SQLException {
        UUID authorId = UUID.randomUUID();
        client.insert(authors).values(authors.create(authorId, "Ada Lovelace")).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId,
                "Notes on the Analytical Engine", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId,
                "Sketch of the Analytical Engine", Post.Status.PUBLISHED)).execute();

        AliasedTable<Author> a = new AliasedTable<>(authors, "a");
        AliasedTable<Post> b = new AliasedTable<>(posts, "b");

        List<AuthorPost> results = client.select(a)
                .join(b).on(a.field(authors.id()).eqField(b.field(posts.authorId())))
                .fetch(row -> new AuthorPost(row.getString(2), row.getString(5)));

        assertEquals(results.size(), 2);
        assertEquals(results.get(0).authorName(), "Ada Lovelace");
    }

    @Test
    public void whereFiltersAcrossTheJoinedRows() throws SQLException {
        UUID authorId = UUID.randomUUID();
        client.insert(authors).values(authors.create(authorId, "Ada Lovelace")).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId,
                "Notes on the Analytical Engine", Post.Status.PUBLISHED)).execute();

        UUID otherAuthorId = UUID.randomUUID();
        client.insert(authors).values(authors.create(otherAuthorId, "Alan Turing")).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), otherAuthorId,
                "On Computable Numbers", Post.Status.PUBLISHED)).execute();

        AliasedTable<Author> a = new AliasedTable<>(authors, "a");
        AliasedTable<Post> b = new AliasedTable<>(posts, "b");

        JoinStep query = client.select(a)
                .join(b).on(a.field(authors.id()).eqField(b.field(posts.authorId())))
                .where(a.field(authors.fullName()).eq("Alan Turing"));

        List<AuthorPost> results = query.fetch(row -> new AuthorPost(row.getString(2), row.getString(5)));

        assertEquals(results.size(), 1);
        assertEquals(results.getFirst().postTitle(), "On Computable Numbers");
    }

    @Test
    public void leftJoinKeepsUnmatchedRowsFromTheLeftSide() throws SQLException {
        UUID authorId = UUID.randomUUID();
        client.insert(authors).values(authors.create(authorId, "Grace Hopper")).execute();

        AliasedTable<Author> a = new AliasedTable<>(authors, "a");
        AliasedTable<Post> b = new AliasedTable<>(posts, "b");

        List<String> results = client.select(a)
                .leftJoin(b).on(a.field(authors.id()).eqField(b.field(posts.authorId())))
                .fetch(row -> row.getString(2));

        assertEquals(results, List.of("Grace Hopper"));
    }

    /// Exercises `fetch(AliasedTable)`.
    @Test
    public void fetchWithAnAliasedTableReturnsPlainEntitiesFilteredByTheJoin() throws SQLException {
        UUID matchingAuthorId = UUID.randomUUID();
        client.insert(authors).values(authors.create(matchingAuthorId, "Ada Lovelace")).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), matchingAuthorId,
                "Notes on the Analytical Engine", Post.Status.PUBLISHED)).execute();

        UUID unmatchedAuthorId = UUID.randomUUID();
        client.insert(authors).values(authors.create(unmatchedAuthorId, "Alan Turing")).execute();

        AliasedTable<Author> a = new AliasedTable<>(authors, "a");
        AliasedTable<Post> b = new AliasedTable<>(posts, "b");

        List<Author> results = client.select(a)
                .join(b).on(a.field(authors.id()).eqField(b.field(posts.authorId())))
                .fetch(a);

        assertEquals(results.size(), 1);
        assertEquals(results.getFirst().fullName(), "Ada Lovelace");
    }

    /// Without `distinct()`, an author matched by two posts would come back twice — one row per
    /// matching join partner.
    @Test
    public void distinctCollapsesOneAuthorMatchedByMultiplePosts() throws SQLException {
        UUID authorId = UUID.randomUUID();
        client.insert(authors).values(authors.create(authorId, "Ada Lovelace")).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId,
                "Notes on the Analytical Engine", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId,
                "Sketch of the Analytical Engine", Post.Status.PUBLISHED)).execute();

        AliasedTable<Author> a = new AliasedTable<>(authors, "a");
        AliasedTable<Post> b = new AliasedTable<>(posts, "b");

        List<Author> withoutDistinct = client.select(a)
                .join(b).on(a.field(authors.id()).eqField(b.field(posts.authorId())))
                .fetch(a);
        List<Author> withDistinct = client.select(a)
                .join(b).on(a.field(authors.id()).eqField(b.field(posts.authorId())))
                .distinct()
                .fetch(a);

        assertEquals(withoutDistinct.size(), 2);
        assertEquals(withDistinct.size(), 1);
    }

    @Test
    public void fetchOneReturnsEmptyWhenNothingMatches() throws SQLException {
        AliasedTable<Author> a = new AliasedTable<>(authors, "a");
        AliasedTable<Post> b = new AliasedTable<>(posts, "b");

        Optional<Author> result = client.select(a)
                .join(b).on(a.field(authors.id()).eqField(b.field(posts.authorId())))
                .fetchOne(a);

        assertEquals(result, Optional.empty());
    }
}
