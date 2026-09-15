package io.github.happyduke96.squell.integration;

import io.github.happyduke96.squell.converter.UuidConverter;
import io.github.happyduke96.squell.execution.SqliteClient;
import io.github.happyduke96.squell.internal.DialectVerifier;
import org.sqlite.SQLiteDataSource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/// Exercises `SqliteClient` against the real `sqlite-jdbc` driver — no Testcontainers, since
/// SQLite is embedded rather than client-server. Uses a temp file, not `:memory:`: `SqliteClient`
/// opens a fresh connection per step, and a true in-memory SQLite database doesn't survive that.
@Test(groups = "integration")
public class SqliteIntegrationTest {

    private DataSource dataSource;
    private SqliteClient client;

    @BeforeClass
    public void openDatabase() throws SQLException, IOException {
        java.nio.file.Path dbFile = Files.createTempFile("squell-integration-", ".sqlite");
        dbFile.toFile().deleteOnExit();

        SQLiteDataSource sqlite = new SQLiteDataSource();
        sqlite.setUrl("jdbc:sqlite:" + dbFile);
        dataSource = sqlite;

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE notes (
                        id TEXT PRIMARY KEY,
                        note TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE flags (
                        id TEXT PRIMARY KEY,
                        enabled INTEGER NOT NULL
                    )
                    """);
        }

        client = new SqliteClient(dataSource);
    }

    private SqliteNoteTable table() {
        return new SqliteNoteTable(new UuidConverter());
    }

    private SqliteFlagTable flagsTable() {
        return new SqliteFlagTable(new UuidConverter());
    }

    @Test
    public void dialectVerifierAcceptsARealSqliteDataSource() throws SQLException {
        new DialectVerifier(dataSource, "SqliteClient", "SQLite", "H2").verify();
    }

    @Test
    public void dialectVerifierRejectsARealSqliteDataSourceWhenNotAccepted() {
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> new DialectVerifier(dataSource, "MySqlClient", "MySQL").verify());

        assertTrue(thrown.getMessage().contains("SQLite"));
    }

    @Test
    public void insertUpdateAndDeleteRoundTripThroughARealSqlite() throws SQLException {
        SqliteNoteTable notes = table();
        UUID id = UUID.randomUUID();

        client.insert(notes)
                .values(notes.create(id, "sqlite-real"))
                .execute();
        assertEquals(client.select(notes)
                .where(notes.id().eq(id))
                .fetch()
                .getFirst()
                .note(), "sqlite-real");

        int updated = client.update(notes)
                .set(notes.note(), "sqlite-updated")
                .where(notes.id().eq(id))
                .execute();
        assertEquals(updated, 1);

        int deleted = client.delete(notes)
                .where(notes.id().eq(id))
                .execute();
        assertEquals(deleted, 1);
        assertTrue(client.select(notes)
                .where(notes.id().eq(id))
                .fetch()
                .isEmpty());
    }

    @Test
    public void executeAndReturnReturnsTheInsertedRowOnRealSqlite() throws SQLException {
        SqliteNoteTable notes = table();
        UUID id = UUID.randomUUID();

        SqliteNote returned = client.insert(notes)
                .values(notes.create(id, "returning-insert"))
                .executeAndReturn();

        assertEquals(returned.note(), "returning-insert");
    }

    @Test
    public void updateAndReturnReturnsTheUpdatedRowOnRealSqlite() throws SQLException {
        SqliteNoteTable notes = table();
        UUID id = UUID.randomUUID();
        client.insert(notes)
                .values(notes.create(id, "before-update"))
                .execute();

        List<SqliteNote> updated = client.update(notes)
                .set(notes.note(), "after-update")
                .where(notes.id().eq(id))
                .updateAndReturn();

        assertEquals(updated.size(), 1);
        assertEquals(updated.getFirst().note(), "after-update");
    }

    @Test
    public void deleteAndReturnReturnsTheDeletedRowOnRealSqlite() throws SQLException {
        SqliteNoteTable notes = table();
        UUID id = UUID.randomUUID();
        client.insert(notes)
                .values(notes.create(id, "to-delete"))
                .execute();

        List<SqliteNote> deleted = client.delete(notes)
                .where(notes.id().eq(id))
                .deleteAndReturn();

        assertEquals(deleted.size(), 1);
        assertEquals(deleted.getFirst().note(), "to-delete");
    }

    @Test
    public void booleanColumnRoundTripsOnRealSqliteDespiteIntegerAffinity() throws SQLException {
        SqliteFlagTable flags = flagsTable();
        UUID trueId = UUID.randomUUID();
        UUID falseId = UUID.randomUUID();

        client.insert(flags)
                .values(flags.create(trueId, true))
                .execute();
        client.insert(flags)
                .values(flags.create(falseId, false))
                .execute();

        assertEquals(client.select(flags)
                .where(flags.id().eq(trueId))
                .fetch()
                .getFirst()
                .enabled(), true);
        assertEquals(client.select(flags)
                .where(flags.id().eq(falseId))
                .fetch()
                .getFirst()
                .enabled(), false);
    }
}
