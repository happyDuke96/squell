package io.github.happyduke96.squell.integration;

import com.mysql.cj.jdbc.MysqlDataSource;
import io.github.happyduke96.squell.converter.UuidConverter;
import io.github.happyduke96.squell.converter.mysql.SetConverter;
import io.github.happyduke96.squell.converter.mysql.VectorConverter;
import io.github.happyduke96.squell.exception.MySqlDeadlockException;
import io.github.happyduke96.squell.exception.MySqlLockNotAvailableException;
import io.github.happyduke96.squell.execution.MySqlClient;
import io.github.happyduke96.squell.internal.DialectVerifier;
import org.testcontainers.containers.MySQLContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
    public void forUpdateNoWaitFailsImmediatelyWithAMySqlLockNotAvailableException() throws SQLException {
        MySqlTypesTable types = table();
        UUID id = UUID.randomUUID();
        client.insert(types).values(types.create(id, new float[] {0f}, Set.of("sql"))).execute();

        try (Connection lockHolder = dataSource.getConnection()) {
            lockHolder.setAutoCommit(false);
            try (PreparedStatement statement = lockHolder.prepareStatement(
                    "SELECT * FROM mysql_types WHERE id = ? FOR UPDATE")) {
                statement.setObject(1, id.toString());
                statement.executeQuery();
            }

            MySqlLockNotAvailableException thrown = expectThrows(MySqlLockNotAvailableException.class,
                    () -> client.select(types).where(types.id().eq(id)).forUpdateNoWait().fetch());
            assertTrue(thrown.retryable());

            lockHolder.commit();
        }

        assertEquals(client.select(types).where(types.id().eq(id)).forUpdateNoWait().fetch().size(), 1);
    }

    @Test
    public void aRealDeadlockBetweenTwoSquellTransactionsThrowsAMySqlDeadlockException() throws Exception {
        MySqlTypesTable types = table();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        client.insert(types).values(types.create(a, new float[] {0f}, Set.of("sql"))).execute();
        client.insert(types).values(types.create(b, new float[] {0f}, Set.of("sql"))).execute();

        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondLocked = new CountDownLatch(1);
        AtomicReference<SQLException> caught = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                client.transaction(tx -> {
                    tx.update(types).set(types.embedding(), new float[] {1f}).where(types.id().eq(a)).execute();
                    firstLocked.countDown();
                    awaitUninterruptibly(secondLocked);
                    return tx.update(types).set(types.embedding(), new float[] {2f}).where(types.id().eq(b))
                            .execute();
                });
            } catch (SQLException e) {
                caught.set(e);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                client.transaction(tx -> {
                    tx.update(types).set(types.embedding(), new float[] {3f}).where(types.id().eq(b)).execute();
                    secondLocked.countDown();
                    awaitUninterruptibly(firstLocked);
                    return tx.update(types).set(types.embedding(), new float[] {4f}).where(types.id().eq(a))
                            .execute();
                });
            } catch (SQLException e) {
                caught.set(e);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertTrue(caught.get() instanceof MySqlDeadlockException,
                "One of the two transactions must lose the deadlock: " + caught.get());
        assertTrue(((MySqlDeadlockException) caught.get()).retryable());
    }

    @Test
    public void transactionWithRetryRetriesAMySqlLockNotAvailableExceptionUntilTheLockClears()
            throws SQLException, InterruptedException {
        MySqlTypesTable types = table();
        UUID id = UUID.randomUUID();
        client.insert(types).values(types.create(id, new float[] {0f}, Set.of("sql"))).execute();
        AtomicInteger attempts = new AtomicInteger();

        try (Connection lockHolder = dataSource.getConnection()) {
            lockHolder.setAutoCommit(false);
            try (PreparedStatement statement = lockHolder.prepareStatement(
                    "SELECT * FROM mysql_types WHERE id = ? FOR UPDATE")) {
                statement.setObject(1, id.toString());
                statement.executeQuery();
            }

            Thread releaseAfterADelay = new Thread(() -> {
                try {
                    Thread.sleep(300);
                    lockHolder.commit();
                } catch (SQLException | InterruptedException ignored) {
                }
            });
            releaseAfterADelay.start();

            List<MySqlTypes> result = client.transactionWithRetry(10, Duration.ofMillis(100), tx -> {
                attempts.incrementAndGet();
                return tx.select(types).where(types.id().eq(id)).forUpdateNoWait().fetch();
            });

            releaseAfterADelay.join();
            assertEquals(result.size(), 1);
            assertTrue(attempts.get() > 1, "Expected at least one retry before the lock was released.");
        }
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
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
