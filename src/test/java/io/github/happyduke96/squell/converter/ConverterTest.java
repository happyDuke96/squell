package io.github.happyduke96.squell.converter;

import io.github.happyduke96.squell.Article;
import io.github.happyduke96.squell.ArticleTable;
import io.github.happyduke96.squell.TagsConverter;
import io.github.happyduke96.squell.execution.PostgresClient;
import org.h2.jdbcx.JdbcDataSource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/// Exercises `@Convert`.
public class ConverterTest {

    private PostgresClient client;
    private ArticleTable articles;

    @BeforeMethod
    public void setUp() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:squell_converter_test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE articles (
                        id UUID PRIMARY KEY,
                        labels VARCHAR NOT NULL
                    )
                    """);
        }

        client = new PostgresClient(dataSource);
        articles = new ArticleTable(new TagsConverter());
    }

    @Test
    public void convertedColumnRoundTripsThroughTheInjectedConverter() throws SQLException {
        UUID id = UUID.randomUUID();
        Article created = articles.create(id, List.of("sql", "java", "oop"));

        client.insert(articles).values(created).execute();

        Article found = client.select(articles).where(articles.id().eq(id)).fetch().getFirst();

        assertEquals(found.labels(), List.of("sql", "java", "oop"));
    }
}
