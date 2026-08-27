package io.github.happyduke96.squell.connection;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionSource {

    Connection acquire() throws SQLException;

    void release(Connection connection) throws SQLException;
}
