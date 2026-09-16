package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.converter.postgres.DurationIntervalConverter;
import io.github.happyduke96.squell.converter.postgres.IntRangeConverter;
import io.github.happyduke96.squell.converter.postgres.JsonbConverter;
import io.github.happyduke96.squell.converter.postgres.Range;
import io.github.happyduke96.squell.converter.postgres.TextArrayConverter;
import io.github.happyduke96.squell.connection.IsolationLevel;
import io.github.happyduke96.squell.converter.InstantConverter;
import io.github.happyduke96.squell.converter.LocalDateConverter;
import io.github.happyduke96.squell.converter.LocalDateTimeConverter;
import io.github.happyduke96.squell.converter.LocalTimeConverter;
import io.github.happyduke96.squell.converter.OffsetDateTimeConverter;
import io.github.happyduke96.squell.converter.ZonedDateTimeConverter;
import io.github.happyduke96.squell.exception.PostgresDeadlockException;
import io.github.happyduke96.squell.exception.PostgresLockNotAvailableException;
import io.github.happyduke96.squell.execution.PostgresClient;
import io.github.happyduke96.squell.internal.DialectVerifier;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/// Exercises `PostgresClient` against a real Postgres — `RETURNING`/`ON CONFLICT` and the
/// Postgres-only converters, which H2 can only fake.
@Test(groups = "integration")
public class PostgresIntegrationTest {

    private PostgreSQLContainer<?> container;
    private DataSource dataSource;
    private PostgresClient client;

    @BeforeClass
    public void startContainer() throws SQLException {
        container = new PostgreSQLContainer<>("postgres:16");
        container.start();

        PGSimpleDataSource pg = new PGSimpleDataSource();
        pg.setUrl(container.getJdbcUrl());
        pg.setUser(container.getUsername());
        pg.setPassword(container.getPassword());
        dataSource = pg;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE inventory (
                        id UUID PRIMARY KEY,
                        sku VARCHAR NOT NULL UNIQUE,
                        quantity INT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE postgres_types (
                        id UUID PRIMARY KEY,
                        payload JSONB NOT NULL,
                        tags TEXT[] NOT NULL,
                        duration INTERVAL NOT NULL,
                        range INT4RANGE NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE events (
                        id UUID PRIMARY KEY,
                        occurredAt TIMESTAMPTZ NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE time_probe (
                        id UUID PRIMARY KEY,
                        localDate DATE NOT NULL,
                        localDateTime TIMESTAMP NOT NULL,
                        wallClockTime TIME NOT NULL,
                        offsetDateTime TIMESTAMPTZ NOT NULL,
                        zonedDateTime TIMESTAMPTZ NOT NULL
                    )
                    """);
        }

        client = new PostgresClient(dataSource);
    }

    @AfterClass
    public void stopContainer() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    public void dialectVerifierAcceptsARealPostgresDataSource() throws SQLException {
        new DialectVerifier(dataSource, "PostgresClient", "PostgreSQL", "H2").verify();
    }

    @Test
    public void dialectVerifierRejectsARealPostgresDataSourceWhenNotAccepted() {
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> new DialectVerifier(dataSource, "SqliteClient", "SQLite").verify());

        assertTrue(thrown.getMessage().contains("PostgreSQL"));
    }

    @Test
    public void executeAndReturnReturnsTheInsertedRow() throws SQLException {
        InventoryItem inserted = new InventoryItemTable().create(UUID.randomUUID(), "SKU-RETURN", 5);

        InventoryItem returned = client.insert(new InventoryItemTable())
                .values(inserted)
                .executeAndReturn();

        assertEquals(returned.sku(), "SKU-RETURN");
        assertEquals(returned.quantity(), 5);
    }

    @Test
    public void onConflictDoUpdateAppliesWhenAConflictOccurs() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID originalId = UUID.randomUUID();
        client.insert(items)
                .values(items.create(originalId, "SKU-CONFLICT", 1))
                .execute();

        InventoryItem attempted = items.create(UUID.randomUUID(), "SKU-CONFLICT", 99);
        InventoryItem result = client.insert(items)
                .values(attempted)
                .onConflict(items.sku())
                .doUpdate(items.quantity());

        assertEquals(result.quantity(), 99);
        assertEquals(result.id(), originalId, "The original row's id must survive an upsert.");
    }

    @Test
    public void onConflictDoUpdateWithNoActualConflictInsertsNormally() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();

        InventoryItem result = client.insert(items)
                .values(items.create(id, "SKU-FRESH-UPDATE", 7))
                .onConflict(items.sku())
                .doUpdate(items.quantity());

        assertEquals(result.id(), id);
        assertEquals(result.quantity(), 7);
    }

    @Test
    public void onConflictDoNothingReturnsEmptyWhenConflictOccurs() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        client.insert(items)
                .values(items.create(UUID.randomUUID(), "SKU-DO-NOTHING", 1))
                .execute();

        Optional<InventoryItem> result = client.insert(items)
                .values(items.create(UUID.randomUUID(), "SKU-DO-NOTHING", 2))
                .onConflict(items.sku())
                .doNothing();

        assertTrue(result.isEmpty());

        InventoryItem unchanged = client.select(items)
                .where(items.sku().eq("SKU-DO-NOTHING"))
                .fetch()
                .getFirst();
        assertEquals(unchanged.quantity(), 1);
    }

    @Test
    public void onConflictDoNothingReturnsTheRowWhenNoConflictOccurs() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();

        Optional<InventoryItem> result = client.insert(items)
                .values(items.create(UUID.randomUUID(), "SKU-FRESH-NOTHING", 3))
                .onConflict(items.sku())
                .doNothing();

        assertTrue(result.isPresent());
        assertEquals(result.get().quantity(), 3);
    }

    @Test
    public void updateAndReturnReturnsTheUpdatedRow() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();
        client.insert(items)
                .values(items.create(id, "SKU-UPDATE-RETURN", 1))
                .execute();

        List<InventoryItem> updated = client.update(items)
                .set(items.quantity(), 42)
                .where(items.id().eq(id))
                .updateAndReturn();

        assertEquals(updated.size(), 1);
        assertEquals(updated.getFirst().quantity(), 42);
    }

    @Test
    public void deleteAndReturnReturnsTheDeletedRow() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();
        client.insert(items)
                .values(items.create(id, "SKU-DELETE-RETURN", 1))
                .execute();

        List<InventoryItem> deleted = client.delete(items)
                .where(items.id().eq(id))
                .deleteAndReturn();

        assertEquals(deleted.size(), 1);
        assertEquals(deleted.getFirst().sku(), "SKU-DELETE-RETURN");
        assertTrue(client.select(items)
                .where(items.id().eq(id))
                .fetch()
                .isEmpty());
    }

    @Test
    public void jsonbColumnRoundTripsThroughARealJsonbType() throws SQLException {
        PostgresTypesTable types = new PostgresTypesTable(new JsonbConverter(), new TextArrayConverter(),
                new DurationIntervalConverter(), new IntRangeConverter());
        UUID id = UUID.randomUUID();

        client.insert(types)
                .values(types.create(id, "{\"lang\":\"java\"}", List.of("sql", "oop"),
                        Duration.ofDays(1).plusHours(2), new Range<>(1, 10)))
                .execute();

        PostgresTypes found = client.select(types)
                .where(types.id().eq(id))
                .fetch()
                .getFirst();

        // Postgres normalizes jsonb's stored text (e.g. inserts whitespace), so this checks
        // content, not the exact byte-for-byte string.
        assertTrue(found.payload().contains("\"lang\"") && found.payload().contains("\"java\""));
    }

    @Test
    public void textArrayColumnRoundTripsThroughARealArrayType() throws SQLException {
        PostgresTypesTable types = new PostgresTypesTable(new JsonbConverter(), new TextArrayConverter(),
                new DurationIntervalConverter(), new IntRangeConverter());
        UUID id = UUID.randomUUID();

        client.insert(types)
                .values(types.create(id, "{}", List.of("sql", "oop", "java"),
                        Duration.ofMinutes(30), new Range<>(0, 1)))
                .execute();

        PostgresTypes found = client.select(types)
                .where(types.id().eq(id))
                .fetch()
                .getFirst();

        assertEquals(found.tags(), List.of("sql", "oop", "java"));
    }

    @Test
    public void intervalColumnRoundTripsThroughARealIntervalType() throws SQLException {
        PostgresTypesTable types = new PostgresTypesTable(new JsonbConverter(), new TextArrayConverter(),
                new DurationIntervalConverter(), new IntRangeConverter());
        UUID id = UUID.randomUUID();
        Duration original = Duration.ofDays(3).plusHours(4).plusMinutes(5).plusSeconds(6);

        client.insert(types)
                .values(types.create(id, "{}", List.of(), original, new Range<>(0, 1)))
                .execute();

        PostgresTypes found = client.select(types)
                .where(types.id().eq(id))
                .fetch()
                .getFirst();

        assertEquals(found.duration(), original);
    }

    @Test
    public void rangeColumnRoundTripsThroughARealRangeType() throws SQLException {
        PostgresTypesTable types = new PostgresTypesTable(new JsonbConverter(), new TextArrayConverter(),
                new DurationIntervalConverter(), new IntRangeConverter());
        UUID id = UUID.randomUUID();

        client.insert(types)
                .values(types.create(id, "{}", List.of(), Duration.ZERO, new Range<>(5, 15)))
                .execute();

        PostgresTypes found = client.select(types)
                .where(types.id().eq(id))
                .fetch()
                .getFirst();

        assertEquals(found.range(), new Range<>(5, 15));
    }

    @Test
    public void instantColumnRoundTripsThroughARealTimestamptzColumnViaInstantConverter() throws SQLException {
        EventTable events = new EventTable(new InstantConverter());
        UUID id = UUID.randomUUID();
        Instant original = Instant.now();

        client.insert(events)
                .values(events.create(id, original))
                .execute();

        Event found = client.select(events)
                .where(events.id().eq(id))
                .fetch()
                .getFirst();

        // TIMESTAMPTZ truncates to microseconds; compare with a tolerance instead of equals().
        assertTrue(Duration.between(original, found.occurredAt()).abs().toMillis() < 1);
    }

    @Test
    public void doUpdateIncrementingAtomicallyBumpsTheCounterOnConflict() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID originalId = UUID.randomUUID();
        client.insert(items)
                .values(items.create(originalId, "SKU-COUNTER", 1))
                .execute();

        InventoryItem attempted = items.create(UUID.randomUUID(), "SKU-COUNTER", 99);
        InventoryItem result = client.insert(items)
                .values(attempted)
                .onConflict(items.sku())
                .doUpdateIncrementing(items.quantity(), 1);

        assertEquals(result.quantity(), 2);
        assertEquals(result.id(), originalId, "The original row's id must survive an upsert.");
    }

    @Test
    public void forUpdateSkipLockedExcludesARowLockedByAnotherRealTransaction() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();
        client.insert(items)
                .values(items.create(id, "SKU-LOCKED", 3))
                .execute();

        try (Connection lockHolder = dataSource.getConnection()) {
            lockHolder.setAutoCommit(false);
            try (PreparedStatement statement = lockHolder.prepareStatement(
                    "SELECT * FROM inventory WHERE id = ? FOR UPDATE")) {
                statement.setObject(1, id);
                statement.executeQuery();
            }

            List<InventoryItem> whileLocked = client.select(items)
                    .where(items.id().eq(id))
                    .forUpdateSkipLocked()
                    .fetch();
            assertTrue(whileLocked.isEmpty());

            lockHolder.commit();
        }

        List<InventoryItem> afterRelease = client.select(items)
                .where(items.id().eq(id))
                .forUpdateSkipLocked()
                .fetch();
        assertEquals(afterRelease.size(), 1);
    }

    @Test
    public void forUpdateNoWaitFailsImmediatelyInsteadOfBlockingOnALockedRow() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();
        client.insert(items)
                .values(items.create(id, "SKU-NOWAIT", 3))
                .execute();

        try (Connection lockHolder = dataSource.getConnection()) {
            lockHolder.setAutoCommit(false);
            try (PreparedStatement statement = lockHolder.prepareStatement(
                    "SELECT * FROM inventory WHERE id = ? FOR UPDATE")) {
                statement.setObject(1, id);
                statement.executeQuery();
            }

            PostgresLockNotAvailableException thrown = expectThrows(PostgresLockNotAvailableException.class,
                    () -> client.select(items)
                            .where(items.id().eq(id))
                            .forUpdateNoWait()
                            .fetch());
            assertTrue(thrown.retryable());

            lockHolder.commit();
        }

        assertEquals(client.select(items)
                .where(items.id().eq(id))
                .forUpdateNoWait()
                .fetch()
                .size(), 1);
    }

    @Test
    public void forShareAllowsAConcurrentSharedLockOnTheSameRow() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();
        client.insert(items)
                .values(items.create(id, "SKU-SHARE", 3))
                .execute();

        try (Connection lockHolder = dataSource.getConnection()) {
            lockHolder.setAutoCommit(false);
            try (PreparedStatement statement = lockHolder.prepareStatement(
                    "SELECT * FROM inventory WHERE id = ? FOR SHARE")) {
                statement.setObject(1, id);
                statement.executeQuery();
            }

            List<InventoryItem> found = client.select(items)
                    .where(items.id().eq(id))
                    .forShare()
                    .fetch();
            assertEquals(found.size(), 1, "A second FOR SHARE lock must not block behind the first.");

            lockHolder.commit();
        }
    }

    @Test
    public void transactionWithIsolationLevelActuallyAppliesItOnTheConnection() throws SQLException {
        String isolation = client.transaction(IsolationLevel.SERIALIZABLE, tx -> tx.sql("SHOW transaction_isolation")
                .result(row -> row.getString(1))
                .getFirst());

        assertEquals(isolation, "serializable");
    }

    @Test
    public void transactionRequiringNewCommitsIndependentlyOfTheOuterRollback() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();

        expectThrows(SQLException.class, () -> client.transaction(tx -> {
            tx.transactionRequiringNew(inner -> {
                inner.insert(items)
                        .values(items.create(id, "SKU-REQUIRES-NEW", 1))
                        .execute();
                return null;
            });
            throw new SQLException("boom");
        }));

        assertEquals(client.select(items)
                .where(items.id().eq(id))
                .fetch()
                .size(), 1);
    }

    @Test
    public void aRealDeadlockBetweenTwoSquellTransactionsThrowsAPostgresDeadlockException() throws Exception {
        InventoryItemTable items = new InventoryItemTable();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        client.insert(items)
                .values(items.create(a, "SKU-DEADLOCK-A", 1))
                .execute();
        client.insert(items)
                .values(items.create(b, "SKU-DEADLOCK-B", 1))
                .execute();

        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondLocked = new CountDownLatch(1);
        AtomicReference<SQLException> caught = new AtomicReference<>();

        Thread t1 = new Thread(() -> {
            try {
                client.transaction(tx -> {
                    tx.update(items)
                            .set(items.quantity(), 10)
                            .where(items.id().eq(a))
                            .execute();
                    firstLocked.countDown();
                    awaitUninterruptibly(secondLocked);
                    return tx.update(items)
                            .set(items.quantity(), 20)
                            .where(items.id().eq(b))
                            .execute();
                });
            } catch (SQLException e) {
                caught.set(e);
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                client.transaction(tx -> {
                    tx.update(items)
                            .set(items.quantity(), 30)
                            .where(items.id().eq(b))
                            .execute();
                    secondLocked.countDown();
                    awaitUninterruptibly(firstLocked);
                    return tx.update(items)
                            .set(items.quantity(), 40)
                            .where(items.id().eq(a))
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

        assertTrue(caught.get() instanceof PostgresDeadlockException,
                "One of the two transactions must lose the deadlock: " + caught.get());
        assertTrue(((PostgresDeadlockException) caught.get()).retryable());
    }

    @Test
    public void transactionWithRetryRetriesAPostgresLockNotAvailableExceptionUntilTheLockClears()
            throws SQLException, InterruptedException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();
        client.insert(items)
                .values(items.create(id, "SKU-RETRY", 1))
                .execute();
        AtomicInteger attempts = new AtomicInteger();

        try (Connection lockHolder = dataSource.getConnection()) {
            lockHolder.setAutoCommit(false);
            try (PreparedStatement statement = lockHolder.prepareStatement(
                    "SELECT * FROM inventory WHERE id = ? FOR UPDATE")) {
                statement.setObject(1, id);
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

            List<InventoryItem> result = client.transactionWithRetry(10, Duration.ofMillis(100), tx -> {
                attempts.incrementAndGet();
                return tx.select(items)
                        .where(items.id().eq(id))
                        .forUpdateNoWait()
                        .fetch();
            });

            releaseAfterADelay.join();
            assertEquals(result.size(), 1);
            assertTrue(attempts.get() > 1, "Expected at least one retry before the lock was released.");
        }
    }

    @Test
    public void transactionNestedRollsBackOnlyItsOwnWriteViaARealSavepoint() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID outerId = UUID.randomUUID();
        UUID nestedId = UUID.randomUUID();

        client.transaction(tx -> {
            tx.insert(items)
                    .values(items.create(outerId, "SKU-NESTED-OUTER", 1))
                    .execute();

            expectThrows(RuntimeException.class, () -> tx.transactionNested(inner -> {
                inner.insert(items)
                        .values(items.create(nestedId, "SKU-NESTED-INNER", 1))
                        .execute();
                throw new RuntimeException("nested failure");
            }));

            return null;
        });

        assertEquals(client.select(items)
                .where(items.id().eq(outerId))
                .fetch()
                .size(), 1, "The outer transaction's own write must survive the nested SAVEPOINT rollback.");
        assertTrue(client.select(items)
                .where(items.id().eq(nestedId))
                .fetch()
                .isEmpty(), "The nested transaction's write must be rolled back to its SAVEPOINT.");
    }

    @Test
    public void everyJavaTimeConverterRoundTripsThroughARealPostgres() throws SQLException {
        TimeProbeTable probes = new TimeProbeTable(new LocalDateConverter(), new LocalDateTimeConverter(),
                new LocalTimeConverter(), new OffsetDateTimeConverter(), new ZonedDateTimeConverter());
        UUID id = UUID.randomUUID();
        LocalDate localDate = LocalDate.of(2024, 1, 15);
        LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0, 123_000_000);
        LocalTime wallClockTime = LocalTime.of(10, 30, 0);
        OffsetDateTime offsetDateTime = OffsetDateTime.of(localDateTime, ZoneOffset.of("+05:00"));
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Tashkent"));

        client.insert(probes)
                .values(probes.create(id, localDate, localDateTime, wallClockTime, offsetDateTime, zonedDateTime))
                .execute();

        TimeProbe found = client.select(probes)
                .where(probes.id().eq(id))
                .fetch()
                .getFirst();

        assertEquals(found.localDate(), localDate);
        assertEquals(found.localDateTime(), localDateTime);
        assertEquals(found.wallClockTime(), wallClockTime);
        // The offset/zone itself doesn't survive the round trip (TIMESTAMPTZ stores an instant,
        // not an offset) — only the absolute instant does, normalized back to UTC.
        assertEquals(found.offsetDateTime().toInstant(), offsetDateTime.toInstant());
        assertEquals(found.zonedDateTime().toInstant(), zonedDateTime.toInstant());
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
