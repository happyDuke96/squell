package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.Comment;
import io.github.happyduke96.squell.CommentTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.condition.Aggregate;
import io.github.happyduke96.squell.condition.Field;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/// Exercises `@GeneratedValue` and the `Aggregate`/`HAVING` surface.
public class CommentTest {

    private PostgresClient client;
    private CommentTable comments;
    private UUID postId;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        comments = new CommentTable();

        UUID authorId = UUID.randomUUID();
        client.insert(new AuthorTable()).values(new AuthorTable().create(authorId, "Ada Lovelace")).execute();

        postId = UUID.randomUUID();
        PostTable posts = new PostTable();
        client.insert(posts).values(posts.create(postId, authorId, "Notes", Post.Status.PUBLISHED)).execute();
    }

    @Test
    public void createReturnsAnEntityWithNoIdYet() {
        Comment draft = comments.create(postId, "tech", 5);

        assertTrue(draft.id().isEmpty());
    }

    @Test
    public void insertableFieldsExcludesTheGeneratedIdColumn() {
        List<String> allNames = comments.fields().stream().map(Field::name).toList();
        List<String> insertableNames = comments.insertableFields().stream().map(Field::name).toList();

        assertEquals(allNames, List.of("id", "postId", "category", "upvotes"));
        assertEquals(insertableNames, List.of("postId", "category", "upvotes"));
    }

    @Test
    public void insertLetsTheDatabaseGenerateTheId() throws SQLException {
        client.insert(comments).values(comments.create(postId, "tech", 5)).execute();

        Comment found = client.select(comments).where(comments.category().eq("tech")).fetch().getFirst();

        assertTrue(found.id().isPresent());
    }

    private void insertSample() throws SQLException {
        client.insert(comments).values(comments.create(postId, "tech", 10)).execute();
        client.insert(comments).values(comments.create(postId, "tech", 20)).execute();
        client.insert(comments).values(comments.create(postId, "science", 5)).execute();
    }

    @Test
    public void countByGroupsRowCountPerCategory() throws SQLException {
        insertSample();

        Map<String, Long> counts = client.select(comments).countBy(comments.category());

        assertEquals(counts.get("tech"), Long.valueOf(2));
        assertEquals(counts.get("science"), Long.valueOf(1));
    }

    @Test
    public void countByWithHavingKeepsOnlyMatchingGroups() throws SQLException {
        insertSample();

        Map<String, Long> counts = client.select(comments)
                .countBy(comments.category(), Aggregate.count().gt(1L));

        assertEquals(counts, Map.of("tech", 2L));
    }

    @Test
    public void sumByAndAvgByComputePerCategoryAggregates() throws SQLException {
        insertSample();

        Map<String, Double> sums = client.select(comments).sumBy(comments.category(), comments.upvotes());
        Map<String, Double> averages = client.select(comments).avgBy(comments.category(), comments.upvotes());

        assertEquals(sums.get("tech"), 30.0, 0.0001);
        assertEquals(sums.get("science"), 5.0, 0.0001);
        assertEquals(averages.get("tech"), 15.0, 0.0001);
        assertEquals(averages.get("science"), 5.0, 0.0001);
    }

    @Test
    public void minByAndMaxByComputePerCategoryExtremes() throws SQLException {
        insertSample();

        Map<String, Integer> minimums = client.select(comments).minBy(comments.category(), comments.upvotes());
        Map<String, Integer> maximums = client.select(comments).maxBy(comments.category(), comments.upvotes());

        assertEquals(minimums.get("tech"), Integer.valueOf(10));
        assertEquals(maximums.get("tech"), Integer.valueOf(20));
        assertEquals(minimums.get("science"), Integer.valueOf(5));
    }

    @Test
    public void ungroupedMinMaxSumAvgComputeAcrossAllMatchingRows() throws SQLException {
        insertSample();

        assertEquals(client.select(comments).min(comments.upvotes()).orElseThrow(), Integer.valueOf(5));
        assertEquals(client.select(comments).max(comments.upvotes()).orElseThrow(), Integer.valueOf(20));
        assertEquals(client.select(comments).sum(comments.upvotes()).orElseThrow(), 35.0, 0.0001);
        assertEquals(client.select(comments).avg(comments.upvotes()).orElseThrow(), 35.0 / 3.0, 0.0001);
    }

    @Test
    public void ungroupedAggregatesAreEmptyWhenNothingMatches() throws SQLException {
        insertSample();

        assertFalse(client.select(comments).where(comments.category().eq("nonexistent")).min(comments.upvotes())
                .isPresent());
    }
}
