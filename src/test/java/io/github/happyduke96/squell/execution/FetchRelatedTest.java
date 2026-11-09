package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.Comment;
import io.github.happyduke96.squell.CommentTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/// Exercises `Client#fetchRelated` — one batched query for the children of several parents,
/// instead of one query per parent.
public class FetchRelatedTest {

    private PostgresClient client;
    private PostTable posts;
    private CommentTable comments;
    private UUID postWithComments;
    private UUID postWithNoComments;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        posts = new PostTable();
        comments = new CommentTable();

        UUID authorId = UUID.randomUUID();
        client.insert(new AuthorTable())
                .values(new AuthorTable().create(authorId, "Ada Lovelace"))
                .execute();

        postWithComments = UUID.randomUUID();
        postWithNoComments = UUID.randomUUID();
        client.insert(posts)
                .values(posts.create(postWithComments, authorId, "Notes", Post.Status.PUBLISHED))
                .execute();
        client.insert(posts)
                .values(posts.create(postWithNoComments, authorId, "Empty", Post.Status.PUBLISHED))
                .execute();

        client.insert(comments)
                .values(comments.create(postWithComments, "tech", 1))
                .execute();
        client.insert(comments)
                .values(comments.create(postWithComments, "tech", 2))
                .execute();
    }

    @Test
    public void groupsChildrenByTheirForeignKey() throws SQLException {
        List<Post> fetchedPosts = client.select(posts)
                .fetch();

        Map<UUID, List<Comment>> commentsByPost = client.fetchRelated(
                fetchedPosts.stream().map(Post::id).toList(),
                comments, comments.postId(), Comment::postId);

        assertEquals(commentsByPost.get(postWithComments).size(), 2);
        assertTrue(commentsByPost.getOrDefault(postWithNoComments, List.of()).isEmpty());
    }

    @Test
    public void anEmptyParentIdCollectionSkipsTheQueryAndReturnsAnEmptyMap() throws SQLException {
        Map<UUID, List<Comment>> commentsByPost = client.fetchRelated(
                List.of(), comments, comments.postId(), Comment::postId);

        assertTrue(commentsByPost.isEmpty());
    }
}
