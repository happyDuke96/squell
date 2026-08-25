package io.github.happyduke96.squell.support;

import io.github.happyduke96.squell.internal.LoggingConnection;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/// Wraps a `DataSource` to log every SQL statement sent through it, at `level` and shaped by
/// `formatter`.
public final class LoggingDataSource implements DataSource {

    private final DataSource origin;
    private final System.Logger.Level level;
    private final SqlFormatter formatter;

    public LoggingDataSource(DataSource origin) {
        this(origin, System.Logger.Level.DEBUG);
    }

    public LoggingDataSource(DataSource origin, System.Logger.Level level) {
        this(origin, level, new NoSqlFormatter());
    }

    public LoggingDataSource(DataSource origin, System.Logger.Level level, SqlFormatter formatter) {
        this.origin = origin;
        this.level = level;
        this.formatter = formatter;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return new LoggingConnection(origin.getConnection(), level, formatter);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return new LoggingConnection(origin.getConnection(username, password), level, formatter);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return origin.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        origin.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        origin.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return origin.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return origin.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return origin.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return origin.isWrapperFor(iface);
    }
}
