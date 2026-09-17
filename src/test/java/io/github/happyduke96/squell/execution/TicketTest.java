package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.Ticket;
import io.github.happyduke96.squell.TicketTable;
import io.github.happyduke96.squell.TestDatabase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/// Exercises `@GeneratedValue` on a primitive (`long id()`) — the zero-default codegen path.
public class TicketTest {

    private PostgresClient client;
    private TicketTable tickets;

    @BeforeMethod
    public void setUp() throws SQLException {
        client = new TestDatabase().postgresClient();
        tickets = new TicketTable();
    }

    @Test
    public void createReturnsAnEntityWithTheZeroDefaultId() {
        Ticket draft = tickets.create("first contact");

        assertEquals(draft.id(), 0L);
    }

    @Test
    public void insertLetsTheDatabaseGenerateTheId() throws SQLException {
        client.insert(tickets)
                .values(tickets.create("first contact"))
                .execute();

        Ticket found = client.select(tickets)
                .fetch()
                .getFirst();

        assertTrue(found.id() > 0);
    }
}
