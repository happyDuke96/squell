package io.github.happyduke96.squell.connection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class PerCallConnection implements ConnectionSource {

    private final DataSource dataSource;

    public PerCallConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection acquire() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void release(Connection connection) throws SQLException {
        connection.close();
    }
}
