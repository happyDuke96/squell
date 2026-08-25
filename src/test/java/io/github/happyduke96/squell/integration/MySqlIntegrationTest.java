package io.github.happyduke96.squell.integration;

import com.mysql.cj.jdbc.MysqlDataSource;
import io.github.happyduke96.squell.converter.UuidConverter;
import io.github.happyduke96.squell.converter.mysql.SetConverter;
import io.github.happyduke96.squell.converter.mysql.VectorConverter;
import io.github.happyduke96.squell.execution.MySqlClient;
import io.github.happyduke96.squell.internal.DialectVerifier;
import org.testcontainers.containers.MySQLContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/// Exercises `MySqlClient` against a real MySQL — the plain CRUD path, plus the MySQL-only
/// converters through their real column types.
@Test(groups = "integration")
public class MySqlIntegrationTest {

    private MySQLContainer<?> container;
    private DataSource dataSource;
    private MySqlClient client;

    @BeforeClass
    public void startContainer() throws SQLException {
        container = new MySQLContainer<>("mysql:8.0");
        container.start();

        MysqlDataSource mysql = new MysqlDataSource();
        mysql.setUrl(container.getJdbcUrl());
        mysql.setUser(container.getUsername());
        mysql.setPassword(container.getPassword());
        dataSource = mysql;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE mysql_types (
                        id CHAR(36) PRIMARY KEY,
                        embedding VARBINARY(255) NOT NULL,
                        labels SET('sql','java','oop','mysql') NOT NULL
                    )
                    """);
        }

        client = new MySqlClient(dataSource);
    }

    @AfterClass
    public void stopContainer() {
        if (container != null) {
            container.stop();
        }
    }

    private MySqlTypesTable table() {
        return new MySqlTypesTable(new UuidConverter(), new VectorConverter(), new SetConverter());
    }

    @Test
    public void dialectVerifierAcceptsARealMySqlDataSource() throws SQLException {
        new DialectVerifier(dataSource, "MySqlClient", "MySQL", "H2").verify();
    }

    @Test
    public void dialectVerifierRejectsARealMySqlDataSourceWhenNotAccepted() {
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> new DialectVerifier(dataSource, "PostgresClient", "PostgreSQL").verify());

        assertTrue(thrown.getMessage().contains("MySQL"));
    }

    @Test
    public void insertUpdateAndDeleteRoundTripThroughARealMySql() throws SQLException {
        MySqlTypesTable types = table();
        UUID id = UUID.randomUUID();

        client.insert(types).values(types.create(id, new float[] {1f}, Set.of("sql"))).execute();
        assertEquals(client.select(types).where(types.id().eq(id)).fetch().getFirst().embedding(),
                new float[] {1f});

        int updated = client.update(types).set(types.embedding(), new float[] {2f})
                .where(types.id().eq(id)).execute();
        assertEquals(updated, 1);
        assertEquals(client.select(types).where(types.id().eq(id)).fetch().getFirst().embedding(),
                new float[] {2f});

        int deleted = client.delete(types).where(types.id().eq(id)).execute();
        assertEquals(deleted, 1);
        assertTrue(client.select(types).where(types.id().eq(id)).fetch().isEmpty());
    }

    @Test
    public void transactionCommitsAcrossMultipleInserts() throws SQLException {
        MySqlTypesTable types = table();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        client.transactionWithoutResult(tx -> {
            tx.insert(types).values(types.create(first, new float[] {1f}, Set.of("sql"))).execute();
            tx.insert(types).values(types.create(second, new float[] {2f}, Set.of("java"))).execute();
        });

        assertEquals(client.select(types).where(types.id().eq(first)).fetch().size(), 1);
        assertEquals(client.select(types).where(types.id().eq(second)).fetch().size(), 1);
    }

    @Test
    public void vectorColumnRoundTripsThroughARealVarbinaryColumn() throws SQLException {
        MySqlTypesTable types = table();
        UUID id = UUID.randomUUID();
        float[] original = {1.5f, -2.25f, 3f};

        client.insert(types).values(types.create(id, original, Set.of("sql"))).execute();

        MySqlTypes found = client.select(types).where(types.id().eq(id)).fetch().getFirst();

        assertEquals(found.embedding(), original);
    }

    @Test
    public void setColumnRoundTripsThroughARealMySqlSetColumn() throws SQLException {
        MySqlTypesTable types = table();
        UUID id = UUID.randomUUID();
        Set<String> original = new LinkedHashSet<>(List.of("java", "oop", "mysql"));

        client.insert(types).values(types.create(id, new float[] {0f}, original)).execute();

        MySqlTypes found = client.select(types).where(types.id().eq(id)).fetch().getFirst();

        assertEquals(found.labels(), original);
    }
}
