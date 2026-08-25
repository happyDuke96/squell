package io.github.happyduke96.squell.validation;

import io.github.happyduke96.squell.AuthorTable;
import io.github.happyduke96.squell.TagTable;
import io.github.happyduke96.squell.execution.PostgresClient;
import org.h2.jdbcx.JdbcDataSource;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/// Exercises `@Column(nonNull = true)`/`@Column(unique = true)` against `validateSchema`.
public class ColumnConstraintsTest {

    private PostgresClient clientFor(String ddl) throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:squell_constraints_test_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
        return new PostgresClient(dataSource);
    }

    @Test
    public void satisfiedConstraintsProduceNoIssues() throws SQLException {
        PostgresClient client = clientFor("""
                CREATE TABLE tags (
                    id UUID PRIMARY KEY,
                    tag_label VARCHAR NOT NULL UNIQUE
                )
                """);

        List<String> issues = client.validateSchema(new TagTable());

        assertTrue(issues.isEmpty(), issues.toString());
    }

    @Test
    public void nullableColumnViolatesNonNull() throws SQLException {
        PostgresClient client = clientFor("""
                CREATE TABLE tags (
                    id UUID PRIMARY KEY,
                    tag_label VARCHAR UNIQUE
                )
                """);

        List<String> issues = client.validateSchema(new TagTable());

        assertTrue(issues.stream().anyMatch(issue -> issue.contains("[tag_label]") && issue.contains("nonNull")));
    }

    @Test
    public void validateSchemaOrThrowThrowsWhenIssuesArePresent() throws SQLException {
        PostgresClient client = clientFor("""
                CREATE TABLE tags (
                    id UUID PRIMARY KEY,
                    tag_label VARCHAR UNIQUE
                )
                """);

        IllegalStateException thrown = expectThrows(IllegalStateException.class,
                () -> client.validateSchemaOrThrow(new TagTable()));

        assertTrue(thrown.getMessage().contains("[tag_label]"));
    }

    @Test
    public void columnWithoutAUniqueIndexViolatesUnique() throws SQLException {
        PostgresClient client = clientFor("""
                CREATE TABLE tags (
                    id UUID PRIMARY KEY,
                    tag_label VARCHAR NOT NULL
                )
                """);

        List<String> issues = client.validateSchema(new TagTable());

        assertTrue(issues.stream().anyMatch(issue -> issue.contains("[tag_label]") && issue.contains("unique")));
    }

    /// A composite unique index doesn't satisfy `unique = true` — deliberately conservative, so
    /// this documents the trade-off rather than hiding it.
    @Test
    public void compositeUniqueIndexDoesNotSatisfySingleColumnUnique() throws SQLException {
        PostgresClient client = clientFor("""
                CREATE TABLE tags (
                    id UUID PRIMARY KEY,
                    tag_label VARCHAR NOT NULL,
                    extra VARCHAR NOT NULL,
                    UNIQUE (tag_label, extra)
                )
                """);

        List<String> issues = client.validateSchema(new TagTable());

        assertTrue(issues.stream().anyMatch(issue -> issue.contains("[tag_label]") && issue.contains("unique")));
    }

    @Test
    public void idColumnIsValidatedAsNonNullAndUniqueWithoutAnExplicitColumnAnnotation() throws SQLException {
        PostgresClient client = clientFor("""
                CREATE TABLE authors (
                    id UUID,
                    "fullName" VARCHAR NOT NULL
                )
                """);

        List<String> issues = client.validateSchema(new AuthorTable());

        assertTrue(issues.stream().anyMatch(issue -> issue.contains("[id]") && issue.contains("nonNull")));
        assertTrue(issues.stream().anyMatch(issue -> issue.contains("[id]") && issue.contains("unique")));
    }

    @Test
    public void idColumnDeclaredAsARealPrimaryKeyProducesNoIssues() throws SQLException {
        PostgresClient client = clientFor("""
                CREATE TABLE authors (
                    id UUID PRIMARY KEY,
                    "fullName" VARCHAR NOT NULL
                )
                """);

        List<String> issues = client.validateSchema(new AuthorTable());

        assertTrue(issues.isEmpty(), issues.toString());
    }
}
