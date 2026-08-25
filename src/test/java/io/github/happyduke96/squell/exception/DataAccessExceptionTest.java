package io.github.happyduke96.squell.exception;

import io.github.happyduke96.squell.execution.PostgresClient;
import org.h2.jdbcx.JdbcDataSource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/// Exercises `DataAccessException.translate` against real constraint violations, plus SQLState
/// mapping that H2 doesn't reproduce on demand (deadlock/lock-timeout).
public class DataAccessExceptionTest {

    private PostgresClient client;

    @BeforeMethod
    public void setUp() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:squell_dae_test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE items (
                        id UUID PRIMARY KEY,
                        name VARCHAR NOT NULL,
                        amount INT CHECK (amount > 0)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE parents (
                        id UUID PRIMARY KEY
                    )
                    """);
            statement.execute("""
                    CREATE TABLE children (
                        id UUID PRIMARY KEY,
                        parent_id UUID REFERENCES parents(id)
                    )
                    """);
        }

        client = new PostgresClient(dataSource);
    }

    @Test
    public void nullInANotNullColumnThrowsNotNullViolationException() {
        NotNullViolationException thrown = expectThrows(NotNullViolationException.class,
                () -> client.sql("INSERT INTO items (id, name, amount) VALUES (?, ?, ?)",
                        UUID.randomUUID(), null, 1).execute());

        assertFalse(thrown.retryable());
    }

    @Test
    public void negativeAmountThrowsCheckViolationException() {
        CheckViolationException thrown = expectThrows(CheckViolationException.class,
                () -> client.sql("INSERT INTO items (id, name, amount) VALUES (?, ?, ?)",
                        UUID.randomUUID(), "widget", -1).execute());

        assertFalse(thrown.retryable());
    }

    @Test
    public void danglingParentIdThrowsForeignKeyViolationException() {
        ForeignKeyViolationException thrown = expectThrows(ForeignKeyViolationException.class,
                () -> client.sql("INSERT INTO children (id, parent_id) VALUES (?, ?)",
                        UUID.randomUUID(), UUID.randomUUID()).execute());

        assertFalse(thrown.retryable());
    }

    @Test
    public void deadlockSqlStateTranslatesToARetryableTransientException() {
        SQLException simulatedDeadlock = new SQLException("Deadlock detected", "40001");

        DataAccessException translated = DataAccessException.translate(simulatedDeadlock);

        assertTrue(translated instanceof TransientDataAccessException);
        assertTrue(translated.retryable());
    }

    @Test
    public void lockTimeoutSqlStateTranslatesToARetryableTransientException() {
        SQLException simulatedTimeout = new SQLException("Lock wait timeout", "HYT00");

        DataAccessException translated = DataAccessException.translate(simulatedTimeout);

        assertTrue(translated instanceof TransientDataAccessException);
        assertTrue(translated.retryable());
    }

    @Test
    public void unrecognizedSqlStateFallsBackToAPlainNonRetryableException() {
        SQLException unmapped = new SQLException("Something else went wrong", "99999");

        DataAccessException translated = DataAccessException.translate(unmapped);

        assertFalse(translated instanceof DuplicateKeyException);
        assertFalse(translated instanceof ForeignKeyViolationException);
        assertFalse(translated instanceof NotNullViolationException);
        assertFalse(translated instanceof CheckViolationException);
        assertFalse(translated instanceof TransientDataAccessException);
        assertFalse(translated.retryable());
    }
}
