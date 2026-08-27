package io.github.happyduke96.squell.connection;

import java.sql.Connection;

public final class SharedConnection implements ConnectionSource {

    private final Connection connection;

    public SharedConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection acquire() {
        return connection;
    }

    @Override
    public void release(Connection connection) {
    }
}
