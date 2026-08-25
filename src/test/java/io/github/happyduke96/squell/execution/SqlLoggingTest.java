package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import io.github.happyduke96.squell.TestDatabase;
import io.github.happyduke96.squell.support.LoggingDataSource;
import io.github.happyduke96.squell.support.NoSqlFormatter;
import io.github.happyduke96.squell.support.PrettySqlFormatter;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/// Exercises `SqlFormatter` and the `LoggingDataSource`/`LoggedSelectStep` decorators.
public class SqlLoggingTest {

    @Test
    public void noSqlFormatterReturnsTheInputUnchanged() {
        assertEquals(new NoSqlFormatter().format("SELECT * FROM posts WHERE id = ?"),
                "SELECT * FROM posts WHERE id = ?");
    }

    @Test
    public void prettySqlFormatterBreaksClausesOntoSeparateLines() {
        String formatted = new PrettySqlFormatter().format("SELECT * FROM posts WHERE id = ? ORDER BY title");

        List<String> lines = formatted.lines().toList();
        assertTrue(lines.getFirst().startsWith("SELECT"));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("FROM")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("WHERE")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("ORDER BY")));
    }

    @Test
    public void loggingDataSourceLogsTheActualSqlSentToTheDatabase() throws SQLException {
        TestDatabase database = new TestDatabase();
        PostgresClient client = new PostgresClient(
                new LoggingDataSource(database.dataSource(), java.lang.System.Logger.Level.INFO,
                        new PrettySqlFormatter()));

        List<LogRecord> captured = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger julLogger = Logger.getLogger("io.github.happyduke96.squell.internal.LoggingConnection");
        julLogger.setLevel(Level.ALL);
        julLogger.addHandler(handler);
        try {
            client.select(new PostTable()).fetch();
        } finally {
            julLogger.removeHandler(handler);
        }

        assertTrue(captured.stream().anyMatch(r -> r.getLevel() == Level.INFO
                && r.getMessage().contains("SELECT")
                && r.getMessage().contains("\nFROM")));
    }

    @Test
    public void loggedSelectStepUsesTheConfiguredLevel() throws SQLException {
        PostgresClient client = new TestDatabase().postgresClient();
        SelectStep<Post> logged = new LoggedSelectStep<>(client.select(new PostTable()),
                java.lang.System.Logger.Level.WARNING);

        List<LogRecord> captured = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                captured.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger julLogger = Logger.getLogger("io.github.happyduke96.squell.execution.LoggedSelectStep");
        julLogger.setLevel(Level.ALL);
        julLogger.addHandler(handler);
        try {
            logged.fetch();
        } finally {
            julLogger.removeHandler(handler);
        }

        assertTrue(captured.stream().anyMatch(r -> r.getLevel() == Level.WARNING));
    }
}
