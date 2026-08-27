package io.github.happyduke96.squell.internal;

import io.github.happyduke96.squell.support.SqlFormatter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class LoggingConnection extends ForwardingConnection {

    private static final System.Logger LOGGER = System.getLogger(LoggingConnection.class.getName());

    private final System.Logger.Level level;
    private final SqlFormatter formatter;

    public LoggingConnection(Connection origin, System.Logger.Level level, SqlFormatter formatter) {
        super(origin);
        this.level = level;
        this.formatter = formatter;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        LOGGER.log(level, formatter.format(sql));
        return super.prepareStatement(sql);
    }
}
