package io.github.happyduke96.squell.internal;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class DialectVerifier {

    private final DataSource dataSource;
    private final String clientName;
    private final List<String> acceptedProductNames;

    public DialectVerifier(DataSource dataSource, String clientName, String... acceptedProductNames) {
        this.dataSource = dataSource;
        this.clientName = clientName;
        this.acceptedProductNames = List.of(acceptedProductNames);
    }

    public void verify() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            String actual = connection.getMetaData().getDatabaseProductName();
            for (String accepted : acceptedProductNames) {
                if (accepted.equalsIgnoreCase(actual)) {
                    return;
                }
            }
            throw new IllegalArgumentException(clientName + " expected " + String.join("/", acceptedProductNames)
                    + " but connected to [" + actual + "] — use the matching client for this database.");
        }
    }
}
