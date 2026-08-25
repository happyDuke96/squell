package io.github.happyduke96.squell;

import io.github.happyduke96.squell.execution.MySqlClient;
import io.github.happyduke96.squell.execution.PostgresClient;
import io.github.happyduke96.squell.execution.SqliteClient;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/// A fresh, schema-populated H2 database (`authors`/`posts`/`comments`/`tags`/`articles`) — for
/// tests that don't need a schema of their own.
public final class TestDatabase {

    private final DataSource dataSource;

    public TestDatabase() throws SQLException {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:squell_test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        h2.setUser("sa");
        h2.setPassword("");
        try (Connection connection = h2.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE authors (
                        id UUID PRIMARY KEY,
                        fullName VARCHAR NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE posts (
                        id UUID PRIMARY KEY,
                        authorId UUID NOT NULL REFERENCES authors(id),
                        title VARCHAR NOT NULL,
                        status VARCHAR NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE comments (
                        id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
                        postId UUID NOT NULL REFERENCES posts(id),
                        category VARCHAR NOT NULL,
                        upvotes INT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE tags (
                        id UUID PRIMARY KEY,
                        tag_label VARCHAR NOT NULL UNIQUE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE articles (
                        id UUID PRIMARY KEY,
                        labels VARCHAR NOT NULL
                    )
                    """);
        }
        this.dataSource = h2;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public PostgresClient postgresClient() throws SQLException {
        return new PostgresClient(dataSource);
    }

    public MySqlClient mySqlClient() throws SQLException {
        return new MySqlClient(dataSource);
    }

    public SqliteClient sqliteClient() throws SQLException {
        return new SqliteClient(dataSource);
    }
}
