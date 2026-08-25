package io.github.happyduke96.squell.validation;

import io.github.happyduke96.squell.internal.DialectVerifier;
import org.h2.jdbcx.JdbcDataSource;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.UUID;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/// Exercises `DialectVerifier`.
public class DialectVerifierTest {

    private JdbcDataSource h2DataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:squell_dialect_test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Test
    public void verifyPassesWhenTheActualProductIsAccepted() throws SQLException {
        new DialectVerifier(h2DataSource(), "TestClient", "H2").verify();
    }

    @Test
    public void verifyFailsWhenTheActualProductIsNotAccepted() {
        IllegalArgumentException thrown = expectThrows(IllegalArgumentException.class,
                () -> new DialectVerifier(h2DataSource(), "SqliteClient", "SQLite").verify());

        assertTrue(thrown.getMessage().contains("SqliteClient"));
        assertTrue(thrown.getMessage().contains("SQLite"));
        assertTrue(thrown.getMessage().contains("H2"));
    }
}
