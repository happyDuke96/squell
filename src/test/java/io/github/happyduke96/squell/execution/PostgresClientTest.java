package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.Account;
import io.github.happyduke96.squell.AccountTable;
import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.condition.Field;
import io.github.happyduke96.squell.connection.IsolationLevel;
import io.github.happyduke96.squell.condition.SubQuery;
import io.github.happyduke96.squell.sql.Table;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;

/// Exercises [PostgresClient] end to end against an embedded H2 database.
public class PostgresClientTest {

    private PostgresClient client;
    private PostTable posts;
    private UUID authorId;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        posts = new PostTable();

        authorId = UUID.randomUUID();
        client.insert(new AuthorTable()).values(new AuthorTable().create(authorId, "Ada Lovelace")).execute();
    }

    @Test
    public void insertThenFetchWithWhereFindsTheInsertedRow() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "Zero-reflection SQL", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).where(posts.id().eq(id)).fetch();

        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().title(), "Zero-reflection SQL");
    }

    @Test
    public void byIdComesFreeFromTheBaseTableInterfaceWithNoCodegenForIt() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "found-via-byId", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).where(posts.byId(id)).fetch();

        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().title(), "found-via-byId");
    }

    @Test
    public void whereCombinesWithAndAcrossMultipleCalls() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "Zero-reflection SQL", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts)
                .where(posts.title().eq("Zero-reflection SQL"))
                .where(posts.status().eq(Post.Status.PUBLISHED))
                .fetch();

        assertEquals(found.size(), 1);
    }

    @Test
    public void whereReturnsANewStepAndLeavesTheOriginalUnfiltered() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "draft-post", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "published-post", Post.Status.PUBLISHED)).execute();

        SelectStep<Post> base = client.select(posts);
        SelectStep<Post> onlyPublished = base.where(posts.status().eq(Post.Status.PUBLISHED));

        assertEquals(base.fetch().size(), 2);
        assertEquals(onlyPublished.fetch().size(), 1);
    }

    @Test
    public void loggedSelectStepDecoratesWithoutChangingTheResult() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "decorated-fetch", Post.Status.PUBLISHED)).execute();

        SelectStep<Post> decorated = new LoggedSelectStep<>(client.select(posts))
                .where(posts.id().eq(id));

        List<Post> found = decorated.fetch();

        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().title(), "decorated-fetch");
    }

    @Test
    public void selectWithNoWhereCallFetchesEveryRow() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "a", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "b", Post.Status.PUBLISHED)).execute();

        assertEquals(client.select(posts).fetch().size(), 2);
    }

    private void insertNumberedPosts(int count) throws SQLException {
        for (int i = 0; i < count; i++) {
            client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "post-" + i, Post.Status.DRAFT))
                    .execute();
        }
    }

    @Test
    public void fetchPageBundlesTheSlicedContentWithTheTotalCount() throws SQLException {
        insertNumberedPosts(5);

        Page<Post> firstPage = client.select(posts).orderBy(posts.title()).fetchPage(0, 2);

        assertEquals(firstPage.content().size(), 2);
        assertEquals(firstPage.totalElements(), 5);
        assertEquals(firstPage.totalPages(), 3);
        assertTrue(firstPage.hasNext());
    }

    @Test
    public void fetchPageOnTheLastPageReportsNoNext() throws SQLException {
        insertNumberedPosts(5);

        Page<Post> lastPage = client.select(posts).orderBy(posts.title()).fetchPage(2, 2);

        assertEquals(lastPage.content().size(), 1);
        assertFalse(lastPage.hasNext());
    }

    @Test
    public void fetchPageRejectsANonPositivePageSize() throws SQLException {
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> client.select(posts).fetchPage(0, 0));

        assertEquals(thrown.getMessage(), "pageSize must be positive, got [0].");
    }

    @Test
    public void fetchSliceFetchesOneExtraRowToDeriveHasNextWithoutCounting() throws SQLException {
        insertNumberedPosts(3);

        Slice<Post> slice = client.select(posts).orderBy(posts.title()).fetchSlice(2);

        assertEquals(slice.content().size(), 2);
        assertTrue(slice.hasNext());
    }

    @Test
    public void fetchSliceOnTheLastPageReportsNoNext() throws SQLException {
        insertNumberedPosts(3);

        Slice<Post> slice = client.select(posts).orderBy(posts.title()).fetchSlice(10);

        assertEquals(slice.content().size(), 3);
        assertFalse(slice.hasNext());
    }

    @Test
    public void fetchSliceCombinesWithAKeysetWhereConditionForCursorPagination() throws SQLException {
        insertNumberedPosts(5);

        Slice<Post> firstSlice = client.select(posts).orderBy(posts.title()).fetchSlice(2);
        String cursor = firstSlice.content().getLast().title();

        Slice<Post> secondSlice = client.select(posts)
                .where(posts.title().gt(cursor))
                .orderBy(posts.title())
                .fetchSlice(2);

        assertEquals(secondSlice.content().size(), 2);
        assertEquals(secondSlice.content().getFirst().title(), "post-2");
    }

    @Test
    public void updateChangesOnlyTheMatchingRow() throws SQLException {
        UUID matching = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        client.insert(posts).values(posts.create(matching, authorId, "draft-post", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(other, authorId, "other-draft", Post.Status.DRAFT)).execute();

        int updated = client.update(posts)
                .set(posts.status(), Post.Status.PUBLISHED)
                .where(posts.id().eq(matching))
                .execute();

        assertEquals(updated, 1);
        assertEquals(client.select(posts).where(posts.id().eq(matching)).fetch().getFirst().status(),
                Post.Status.PUBLISHED);
        assertEquals(client.select(posts).where(posts.id().eq(other)).fetch().getFirst().status(),
                Post.Status.DRAFT);
    }

    @Test
    public void setReturnsANewStepAndLeavesTheOriginalWithoutThatAssignment() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "immutable-update", Post.Status.DRAFT)).execute();

        UpdateStep<Post> withStatus = client.update(posts).set(posts.status(), Post.Status.PUBLISHED);
        UpdateStep<Post> withStatusAndTitle = withStatus.set(posts.title(), "renamed");

        withStatusAndTitle.where(posts.id().eq(id)).execute();

        Post reloaded = client.select(posts).where(posts.id().eq(id)).fetch().getFirst();
        assertEquals(reloaded.status(), Post.Status.PUBLISHED);
        assertEquals(reloaded.title(), "renamed");
    }

    @Test
    public void setWithAnotherFieldCopiesItsValueOnTheSameRow() throws SQLException {
        AccountTable accounts = new AccountTable();
        UUID id = UUID.randomUUID();
        client.insert(accounts).values(accounts.create(id, 100, 25)).execute();

        client.update(accounts).set(accounts.balance(), accounts.pendingBalance())
                .where(accounts.id().eq(id))
                .execute();

        Account reloaded = client.select(accounts).where(accounts.id().eq(id)).fetch().getFirst();
        assertEquals(reloaded.balance(), 25);
        assertEquals(reloaded.pendingBalance(), 25);
    }

    @Test
    public void updateWithNoSetCallFailsClearly() {
        IllegalStateException thrown = expectThrows(IllegalStateException.class,
                () -> client.update(posts).where(posts.id().eq(UUID.randomUUID())).execute());

        assertEquals(thrown.getMessage(), "UPDATE [posts] requires at least one set(...) call.");
    }

    @Test
    public void updateAndReturnWithNoSetCallFailsClearly() {
        IllegalStateException thrown = expectThrows(IllegalStateException.class,
                () -> client.update(posts).where(posts.id().eq(UUID.randomUUID())).updateAndReturn());

        assertEquals(thrown.getMessage(), "UPDATE [posts] requires at least one set(...) call.");
    }

    @Test
    public void deleteRemovesOnlyTheMatchingRow() throws SQLException {
        UUID matching = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        client.insert(posts).values(posts.create(matching, authorId, "delete-me", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(other, authorId, "keep-me", Post.Status.DRAFT)).execute();

        int deleted = client.delete(posts).where(posts.id().eq(matching)).execute();

        assertEquals(deleted, 1);
        assertEquals(client.select(posts).fetch().size(), 1);
        assertEquals(client.select(posts).fetch().getFirst().id(), other);
    }

    @Test
    public void deleteWithNoWhereCallFailsClearly() {
        IllegalStateException thrown = expectThrows(IllegalStateException.class,
                () -> client.delete(posts).execute());

        assertEquals(thrown.getMessage(), "DELETE FROM [posts] requires where(...) with an actual condition.");
    }

    @Test
    public void deleteAndReturnWithNoWhereCallFailsClearly() {
        IllegalStateException thrown = expectThrows(IllegalStateException.class,
                () -> client.delete(posts).deleteAndReturn());

        assertEquals(thrown.getMessage(), "DELETE FROM [posts] requires where(...) with an actual condition.");
    }

    @Test
    public void orderByOrdersResultsAscendingBySpecifiedField() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "charlie", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "alpha", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "bravo", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).orderBy(posts.title()).fetch();

        assertEquals(found.stream().map(Post::title).toList(), List.of("alpha", "bravo", "charlie"));
    }

    @Test
    public void orderByDescOrdersResultsDescendingBySpecifiedField() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "charlie", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "alpha", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "bravo", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).orderByDesc(posts.title()).fetch();

        assertEquals(found.stream().map(Post::title).toList(), List.of("charlie", "bravo", "alpha"));
    }

    @Test
    public void orderByAddsASecondSortKeyInsteadOfReplacingTheFirst() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "multi-b", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "multi-a", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "multi-c", Post.Status.DRAFT)).execute();

        List<Post> found = client.select(posts).orderBy(posts.status()).orderBy(posts.title()).fetch();

        assertEquals(found.stream().map(Post::title).toList(), List.of("multi-c", "multi-a", "multi-b"));
    }

    @Test
    public void limitAndOffsetPageThroughOrderedResults() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "alpha", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "bravo", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "charlie", Post.Status.PUBLISHED)).execute();

        List<Post> secondRow = client.select(posts).orderBy(posts.title()).limit(1).offset(1).fetch();

        assertEquals(secondRow.size(), 1);
        assertEquals(secondRow.getFirst().title(), "bravo");
    }

    @Test
    public void negativeLimitIsRejected() {
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> client.select(posts).limit(-1));

        assertEquals(thrown.getMessage(), "limit must not be negative, got [-1].");
    }

    @Test
    public void negativeOffsetIsRejected() {
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> client.select(posts).offset(-1));

        assertEquals(thrown.getMessage(), "offset must not be negative, got [-1].");
    }

    @Test
    public void distinctRemovesDuplicateValuesFromFetchColumn() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "a", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "b", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "c", Post.Status.DRAFT)).execute();

        List<Post.Status> statuses = client.select(posts).distinct().fetchColumn(posts.status());

        assertEquals(statuses.size(), 2);
    }

    @Test
    public void fetchOneReturnsEmptyWhenNoRowMatches() throws SQLException {
        Optional<Post> found = client.select(posts).where(posts.title().eq("does-not-exist")).fetchOne();

        assertTrue(found.isEmpty());
    }

    @Test
    public void fetchOneThrowsWhenMoreThanOneRowMatches() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "dup", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "dup", Post.Status.PUBLISHED)).execute();

        IllegalStateException thrown = expectThrows(IllegalStateException.class,
                () -> client.select(posts).where(posts.title().eq("dup")).fetchOne());

        assertEquals(thrown.getMessage(), "Query for [posts] returned more than one row.");
    }

    @Test
    public void fetchColumnProjectsOntoOneFieldInOrderByOrder() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "charlie", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "alpha", Post.Status.PUBLISHED)).execute();

        List<String> titles = client.select(posts).orderBy(posts.title()).fetchColumn(posts.title());

        assertEquals(titles, List.of("alpha", "charlie"));
    }

    @Test
    public void countReturnsTheNumberOfMatchingRows() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "a", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "b", Post.Status.DRAFT)).execute();

        long count = client.select(posts).where(posts.status().eq(Post.Status.PUBLISHED)).count();

        assertEquals(count, 1);
    }

    @Test
    public void transactionCommitsAllStepsOnSuccess() throws SQLException {
        client.transaction(tx -> {
            tx.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "commit-1", Post.Status.DRAFT)).execute();
            tx.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "commit-2", Post.Status.DRAFT)).execute();
            return null;
        });

        assertEquals(client.select(posts).fetch().size(), 2);
    }

    @Test
    public void transactionRollsBackAllStepsOnException() throws SQLException {
        try {
            client.transaction(tx -> {
                tx.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "rollback-1", Post.Status.DRAFT)).execute();
                throw new IllegalStateException("simulated failure after the insert");
            });
        } catch (IllegalStateException expected) {
            // Expected — asserting the rollback below is the actual point of this test.
        }

        assertEquals(client.select(posts).fetch().size(), 0);
    }

    @Test
    public void transactionWithoutResultCommitsOnSuccess() throws SQLException {
        UUID id = UUID.randomUUID();
        client.transactionWithoutResult(tx ->
                tx.insert(posts).values(posts.create(id, authorId, "tx-without-result", Post.Status.PUBLISHED)).execute());

        assertEquals(client.select(posts).where(posts.id().eq(id)).fetch().size(), 1);
    }

    @Test
    public void rawSqlQueryMapsRowsWithAFunctionalRowMapper() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "raw-sql", Post.Status.PUBLISHED)).execute();

        List<String> titles = client.sql("SELECT title FROM posts WHERE status = ?", "PUBLISHED")
                .result(row -> row.getString("title"));

        assertEquals(titles, List.of("raw-sql"));
    }

    @Test
    public void rawSqlUsesTableAsAReadyMadeRowMapperForWholeRows() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "raw-table-mapper", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.sql("SELECT * FROM posts WHERE id = ?", id).result(posts);

        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().title(), "raw-table-mapper");
    }

    @Test
    public void rawSqlUpdateRunsAnArbitraryStatement() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "raw-update", Post.Status.DRAFT)).execute();

        int updated = client.sql("UPDATE posts SET status = ? WHERE title = ?", "PUBLISHED", "raw-update").execute();

        assertEquals(updated, 1);
    }

    @Test
    public void rawSqlRejectsAParamCountMismatch() {
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> client.sql("SELECT * FROM posts WHERE title = ? AND status = ?", "only-one"));

        assertTrue(thrown.getMessage().contains("[2] placeholder(s)"));
        assertTrue(thrown.getMessage().contains("[1] param(s)"));
    }

    @Test
    public void validateSchemaFindsAMissingTable() throws SQLException {
        Table<Object> missingTable = new Table<>() {
            @Override
            public String name() {
                return "missing";
            }

            @Override
            public List<Field<?>> fields() {
                return List.of();
            }

            @Override
            public Object fromRow(ResultSet row) {
                return null;
            }

            @Override
            public List<Object> values(Object entity) {
                return List.of();
            }
        };

        List<String> issues = client.validateSchema(missingTable);

        assertTrue(issues.stream().anyMatch(issue -> issue.equals("Table [missing] does not exist.")));
    }

    @Test
    public void inFiltersByAnyOfSeveralValues() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "a", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "b", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "c", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).where(posts.status().in(List.of(Post.Status.PUBLISHED))).fetch();

        assertEquals(found.size(), 2);
    }

    @Test
    public void notInExcludesMatchingRows() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "a", Post.Status.DRAFT)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "b", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).where(posts.status().notIn(List.of(Post.Status.DRAFT))).fetch();

        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().title(), "b");
    }

    @Test
    public void likeMatchesAPattern() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "Zero-reflection SQL", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "Other post", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).where(posts.title().like("Zero%")).fetch();

        assertEquals(found.size(), 1);
    }

    @Test
    public void notLikeExcludesAMatchingPattern() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "Zero-reflection SQL", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "Other post", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).where(posts.title().notLike("Zero%")).fetch();

        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().title(), "Other post");
    }

    @Test
    public void betweenFiltersByRange() throws SQLException {
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "alpha", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "mid", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "zeta", Post.Status.PUBLISHED)).execute();

        List<Post> found = client.select(posts).where(posts.title().between("b", "n")).fetch();

        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().title(), "mid");
    }

    @Test
    public void inSubQueryFiltersUsingAnEmbeddedSelect() throws SQLException {
        UUID publishedId = UUID.randomUUID();
        client.insert(posts).values(posts.create(publishedId, authorId, "subquery-published", Post.Status.PUBLISHED)).execute();
        client.insert(posts).values(posts.create(UUID.randomUUID(), authorId, "subquery-draft", Post.Status.DRAFT)).execute();

        SubQuery<UUID> publishedIds = client.select(posts)
                .where(posts.status().eq(Post.Status.PUBLISHED))
                .subQuery(posts.id());

        List<Post> found = client.select(posts).where(posts.id().in(publishedIds)).fetch();

        assertEquals(found.size(), 1);
        assertEquals(found.getFirst().id(), publishedId);
    }

    @Test
    public void insertBatchInsertsEveryEntityInOneStatement() throws SQLException {
        List<Post> batch = List.of(
                posts.create(UUID.randomUUID(), authorId, "batch-1", Post.Status.DRAFT),
                posts.create(UUID.randomUUID(), authorId, "batch-2", Post.Status.DRAFT),
                posts.create(UUID.randomUUID(), authorId, "batch-3", Post.Status.PUBLISHED));

        int[] counts = client.insertBatch(posts, batch);

        assertEquals(counts.length, 3);
        assertEquals(client.select(posts).fetch().size(), 3);
    }

    @Test
    public void updateBatchUpdatesEveryEntityMatchedByIdInOneStatement() throws SQLException {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        client.insertBatch(posts, List.of(
                posts.create(first, authorId, "batch-update-1", Post.Status.DRAFT),
                posts.create(second, authorId, "batch-update-2", Post.Status.DRAFT)));

        List<Post> published = List.of(
                posts.create(first, authorId, "batch-update-1", Post.Status.PUBLISHED),
                posts.create(second, authorId, "batch-update-2", Post.Status.PUBLISHED));

        int[] counts = client.updateBatch(posts, published, posts.id());

        assertEquals(counts.length, 2);
        assertTrue(client.select(posts).fetch().stream().allMatch(p -> p.status() == Post.Status.PUBLISHED));
    }

    @Test
    public void forUpdateStillFetchesTheMatchingRowsInsideATransaction() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "locked-row", Post.Status.PUBLISHED)).execute();

        Optional<Post> found = client.transaction(tx -> tx.select(posts)
                .where(posts.id().eq(id))
                .forUpdate()
                .fetchOne());

        assertEquals(found.map(Post::title), Optional.of("locked-row"));
    }

    @Test
    public void forUpdateSkipLockedStillFetchesTheMatchingRowsInsideATransaction() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "skip-locked-row", Post.Status.PUBLISHED)).execute();

        Optional<Post> found = client.transaction(tx -> tx.select(posts)
                .where(posts.id().eq(id))
                .forUpdateSkipLocked()
                .fetchOne());

        assertEquals(found.map(Post::title), Optional.of("skip-locked-row"));
    }

    @Test
    public void forUpdateNoWaitStillFetchesTheMatchingRowsInsideATransaction() throws SQLException {
        UUID id = UUID.randomUUID();
        client.insert(posts).values(posts.create(id, authorId, "nowait-row", Post.Status.PUBLISHED)).execute();

        Optional<Post> found = client.transaction(tx -> tx.select(posts)
                .where(posts.id().eq(id))
                .forUpdateNoWait()
                .fetchOne());

        assertEquals(found.map(Post::title), Optional.of("nowait-row"));
    }

    @Test
    public void transactionWithIsolationLevelStillCommits() throws SQLException {
        UUID id = UUID.randomUUID();

        client.transaction(IsolationLevel.SERIALIZABLE, tx -> {
            tx.insert(posts).values(posts.create(id, authorId, "serializable-row", Post.Status.DRAFT)).execute();
            return null;
        });

        assertEquals(client.select(posts).where(posts.id().eq(id)).fetch().getFirst().title(), "serializable-row");
    }

    @Test
    public void transactionWithoutResultAcceptsAnIsolationLevel() throws SQLException {
        UUID id = UUID.randomUUID();

        client.transactionWithoutResult(IsolationLevel.REPEATABLE_READ, tx ->
                tx.insert(posts).values(posts.create(id, authorId, "repeatable-read-row", Post.Status.DRAFT))
                        .execute());

        assertEquals(client.select(posts).where(posts.id().eq(id)).fetch().getFirst().title(),
                "repeatable-read-row");
    }

    @Test
    public void requiredPropagationSeesUncommittedWritesFromTheOuterTransaction() throws SQLException {
        UUID id = UUID.randomUUID();

        Optional<Post> foundThroughTheNestedRequiredTransaction = client.transaction(tx -> {
            tx.insert(posts).values(posts.create(id, authorId, "required-row", Post.Status.DRAFT)).execute();
            return tx.transaction(inner -> inner.select(posts).where(posts.id().eq(id)).fetchOne());
        });

        assertEquals(foundThroughTheNestedRequiredTransaction.map(Post::title), Optional.of("required-row"));
    }

    @Test
    public void requiredPropagationRollsBackWithTheOuterTransaction() throws SQLException {
        UUID id = UUID.randomUUID();

        expectThrows(SQLException.class, () -> client.transaction(tx -> {
            tx.transaction(inner -> {
                inner.insert(posts).values(posts.create(id, authorId, "required-rollback", Post.Status.DRAFT))
                        .execute();
                return null;
            });
            throw new SQLException("boom");
        }));

        assertTrue(client.select(posts).where(posts.id().eq(id)).fetch().isEmpty());
    }

    @Test
    public void transactionRequiringNewCommitsIndependentlyOfTheOuterRollback() throws SQLException {
        UUID id = UUID.randomUUID();

        expectThrows(SQLException.class, () -> client.transaction(tx -> {
            tx.transactionRequiringNew(inner -> {
                inner.insert(posts).values(posts.create(id, authorId, "requires-new-row", Post.Status.DRAFT))
                        .execute();
                return null;
            });
            throw new SQLException("boom");
        }));

        assertEquals(client.select(posts).where(posts.id().eq(id)).fetch().getFirst().title(), "requires-new-row");
    }

    @Test
    public void transactionNestedRollsBackOnlyItsOwnWriteOnFailure() throws SQLException {
        UUID outerId = UUID.randomUUID();
        UUID nestedId = UUID.randomUUID();

        client.transaction(tx -> {
            tx.insert(posts).values(posts.create(outerId, authorId, "outer-row", Post.Status.DRAFT)).execute();

            expectThrows(RuntimeException.class, () -> tx.transactionNested(inner -> {
                inner.insert(posts).values(posts.create(nestedId, authorId, "nested-row", Post.Status.DRAFT))
                        .execute();
                throw new RuntimeException("nested failure");
            }));

            return null;
        });

        assertEquals(client.select(posts).where(posts.id().eq(outerId)).fetch().size(), 1,
                "The outer transaction's own write must survive the nested rollback.");
        assertTrue(client.select(posts).where(posts.id().eq(nestedId)).fetch().isEmpty(),
                "The nested transaction's write must be rolled back to its savepoint.");
    }

    @Test
    public void transactionNestedWriteStillRollsBackWhenTheOuterTransactionLaterFails() throws SQLException {
        UUID nestedId = UUID.randomUUID();

        expectThrows(SQLException.class, () -> client.transaction(tx -> {
            tx.transactionNested(inner -> {
                inner.insert(posts).values(posts.create(nestedId, authorId, "nested-committed-row",
                        Post.Status.DRAFT)).execute();
                return null;
            });
            throw new SQLException("outer boom");
        }));

        assertTrue(client.select(posts).where(posts.id().eq(nestedId)).fetch().isEmpty(),
                "A released savepoint isn't a real commit — it must still roll back with the outer transaction.");
    }

}
