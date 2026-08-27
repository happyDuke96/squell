package io.github.happyduke96.squell.support;

public final class NoSqlFormatter implements SqlFormatter {

    @Override
    public String format(String sql) {
        return sql;
    }
}
