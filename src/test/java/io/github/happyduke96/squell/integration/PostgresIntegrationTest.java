package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.converter.postgres.DurationIntervalConverter;
import io.github.happyduke96.squell.converter.postgres.IntRangeConverter;
import io.github.happyduke96.squell.converter.postgres.JsonbConverter;
import io.github.happyduke96.squell.converter.postgres.Range;
import io.github.happyduke96.squell.converter.postgres.TextArrayConverter;
import io.github.happyduke96.squell.execution.PostgresClient;
import io.github.happyduke96.squell.internal.DialectVerifier;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        InventoryItem returned = client.insert(new InventoryItemTable()).values(inserted).executeAndReturn();

        assertEquals(returned.sku(), "SKU-RETURN");
        assertEquals(returned.quantity(), 5);
    }

    @Test
    public void onConflictDoUpdateAppliesWhenAConflictOccurs() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID originalId = UUID.randomUUID();
        client.insert(items).values(items.create(originalId, "SKU-CONFLICT", 1)).execute();

        InventoryItem attempted = items.create(UUID.randomUUID(), "SKU-CONFLICT", 99);
        InventoryItem result = client.insert(items).values(attempted)
                .onConflict(items.sku())
                .doUpdate(items.quantity());

        assertEquals(result.quantity(), 99);
        assertEquals(result.id(), originalId, "The original row's id must survive an upsert.");
    }

    @Test
    public void onConflictDoUpdateWithNoActualConflictInsertsNormally() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        UUID id = UUID.randomUUID();

        InventoryItem result = client.insert(items).values(items.create(id, "SKU-FRESH-UPDATE", 7))
                .onConflict(items.sku())
                .doUpdate(items.quantity());

        assertEquals(result.id(), id);
        assertEquals(result.quantity(), 7);
    }

    @Test
    public void onConflictDoNothingReturnsEmptyWhenConflictOccurs() throws SQLException {
        InventoryItemTable items = new InventoryItemTable();
        client.insert(items).values(items.create(UUID.randomUUID(), "SKU-DO-NOTHING", 1)).execute();

        Optional<InventoryItem> result = client.insert(items)
                .values(items.create(UUID.randomUUID(), "SKU-DO-NOTHING", 2))
                .onConflict(items.sku())
                .doNothing();

        assertTrue(result.isEmpty());

        InventoryItem unchanged = client.select(items).where(items.sku().eq("SKU-DO-NOTHING")).fetch().getFirst();
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
        client.insert(items).values(items.create(id, "SKU-UPDATE-RETURN", 1)).execute();

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
        client.insert(items).values(items.create(id, "SKU-DELETE-RETURN", 1)).execute();

        List<InventoryItem> deleted = client.delete(items).where(items.id().eq(id)).deleteAndReturn();

        assertEquals(deleted.size(), 1);
        assertEquals(deleted.getFirst().sku(), "SKU-DELETE-RETURN");
        assertTrue(client.select(items).where(items.id().eq(id)).fetch().isEmpty());
    }

    @Test
    public void jsonbColumnRoundTripsThroughARealJsonbType() throws SQLException {
        PostgresTypesTable types = new PostgresTypesTable(new JsonbConverter(), new TextArrayConverter(),
                new DurationIntervalConverter(), new IntRangeConverter());
        UUID id = UUID.randomUUID();

        client.insert(types).values(types.create(id, "{\"lang\":\"java\"}", List.of("sql", "oop"),
                Duration.ofDays(1).plusHours(2), new Range<>(1, 10))).execute();

        PostgresTypes found = client.select(types).where(types.id().eq(id)).fetch().getFirst();

        // Postgres normalizes jsonb's stored text (e.g. inserts whitespace), so this checks
        // content, not the exact byte-for-byte string.
        assertTrue(found.payload().contains("\"lang\"") && found.payload().contains("\"java\""));
    }

    @Test
    public void textArrayColumnRoundTripsThroughARealArrayType() throws SQLException {
        PostgresTypesTable types = new PostgresTypesTable(new JsonbConverter(), new TextArrayConverter(),
                new DurationIntervalConverter(), new IntRangeConverter());
        UUID id = UUID.randomUUID();

        client.insert(types).values(types.create(id, "{}", List.of("sql", "oop", "java"),
                Duration.ofMinutes(30), new Range<>(0, 1))).execute();

        PostgresTypes found = client.select(types).where(types.id().eq(id)).fetch().getFirst();

        assertEquals(found.tags(), List.of("sql", "oop", "java"));
    }

    @Test
    public void intervalColumnRoundTripsThroughARealIntervalType() throws SQLException {
        PostgresTypesTable types = new PostgresTypesTable(new JsonbConverter(), new TextArrayConverter(),
                new DurationIntervalConverter(), new IntRangeConverter());
        UUID id = UUID.randomUUID();
        Duration original = Duration.ofDays(3).plusHours(4).plusMinutes(5).plusSeconds(6);

        client.insert(types).values(types.create(id, "{}", List.of(), original, new Range<>(0, 1))).execute();

        PostgresTypes found = client.select(types).where(types.id().eq(id)).fetch().getFirst();

        assertEquals(found.duration(), original);
    }

    @Test
    public void rangeColumnRoundTripsThroughARealRangeType() throws SQLException {
        PostgresTypesTable types = new PostgresTypesTable(new JsonbConverter(), new TextArrayConverter(),
                new DurationIntervalConverter(), new IntRangeConverter());
        UUID id = UUID.randomUUID();

        client.insert(types).values(types.create(id, "{}", List.of(), Duration.ZERO, new Range<>(5, 15))).execute();

        PostgresTypes found = client.select(types).where(types.id().eq(id)).fetch().getFirst();

        assertEquals(found.range(), new Range<>(5, 15));
    }
}
