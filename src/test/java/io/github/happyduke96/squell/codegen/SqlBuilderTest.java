package io.github.happyduke96.squell.codegen;

import io.github.happyduke96.squell.internal.SqlBuilder;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/// Exercises `SqlBuilder`.
public class SqlBuilderTest {

    @Test
    public void appendConcatenatesFragmentsInOrder() {
        String sql = new SqlBuilder().append("SELECT * FROM ").append("posts").toString();

        assertEquals(sql, "SELECT * FROM posts");
    }

    @Test
    public void appendIfSkipsTheFragmentWhenConditionIsFalse() {
        String sql = new SqlBuilder().append("SELECT ").appendIf(false, "DISTINCT ").append("* FROM posts")
                .toString();

        assertEquals(sql, "SELECT * FROM posts");
    }

    @Test
    public void appendIfIncludesTheFragmentWhenConditionIsTrue() {
        String sql = new SqlBuilder().append("SELECT ").appendIf(true, "DISTINCT ").append("* FROM posts")
                .toString();

        assertEquals(sql, "SELECT DISTINCT * FROM posts");
    }

    @Test
    public void clauseIsOmittedWhenContentIsEmpty() {
        String sql = new SqlBuilder().append("SELECT * FROM posts").clause("WHERE", "").toString();

        assertEquals(sql, "SELECT * FROM posts");
    }

    @Test
    public void clausePrependsTheKeywordWhenContentIsPresent() {
        String sql = new SqlBuilder().append("SELECT * FROM posts").clause("WHERE", "id = ?").toString();

        assertEquals(sql, "SELECT * FROM posts WHERE id = ?");
    }
}
