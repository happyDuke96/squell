package io.github.happyduke96.squell.execution;

import io.github.happyduke96.squell.sql.RowMapper;

import io.github.happyduke96.squell.connection.ConnectionSource;
import io.github.happyduke96.squell.exception.DataAccessException;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// Runs a raw SQL statement — `sql` as-is, `params` bound positionally to its `?` placeholders.
/// `sql` itself is never escaped or validated beyond counting placeholders — never build it by
/// concatenating untrusted input; only the bound `params` are safe from injection.
public final class RawStep {

    private static final System.Logger LOGGER = System.getLogger(RawStep.class.getName());

    @Language("SQL")
    private final String sql;
    private final Object[] params;
    private final ConnectionSource source;

    public RawStep(@Language("SQL") String sql, Object[] params, ConnectionSource source) {
        int placeholders = countPlaceholders(sql);
        if (placeholders != params.length) {
            throw new IllegalArgumentException("Query has [" + placeholders + "] placeholder(s) but ["
                    + params.length + "] param(s) were given: " + sql);
        }
        this.sql = sql;
        this.params = params;
        this.source = source;
    }

    private static int countPlaceholders(String sql) {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inQuotes) {
                if (c == '\\' && i + 1 < sql.length()) {
                    i++;
                } else if (c == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        i++;
                    } else {
                        inQuotes = false;
                    }
                }
            } else if (c == '\'') {
                inQuotes = true;
            } else if (c == '?') {
                count++;
            }
        }
        return count;
    }

    public <T> List<T> result(RowMapper<T> mapper) throws SQLException {
        Connection connection = source.acquire();
        try {
            LOGGER.log(System.Logger.Level.DEBUG, "Executing query: {0}", sql);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindParams(statement);
                try (ResultSet rows = statement.executeQuery()) {
                    List<T> results = new ArrayList<>();
                    while (rows.next()) {
                        results.add(mapper.map(rows));
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    public <T> Optional<T> resultOne(RowMapper<T> mapper) throws SQLException {
        List<T> results = result(mapper);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Query returned more than one row.");
        }
        return Optional.of(results.getFirst());
    }

    public int execute() throws SQLException {
        Connection connection = source.acquire();
        try {
            LOGGER.log(System.Logger.Level.DEBUG, "Executing query: {0}", sql);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindParams(statement);
                return statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw DataAccessException.translate(e);
        } finally {
            source.release(connection);
        }
    }

    private void bindParams(PreparedStatement statement) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}
