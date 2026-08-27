package io.github.happyduke96.squell.internal;

public final class SqlBuilder {

    private final StringBuilder sql = new StringBuilder();

    public SqlBuilder append(String fragment) {
        sql.append(fragment);
        return this;
    }

    public SqlBuilder appendIf(boolean condition, String fragment) {
        if (condition) {
            sql.append(fragment);
        }
        return this;
    }

    public SqlBuilder clause(String keyword, String content) {
        if (!content.isEmpty()) {
            sql.append(' ').append(keyword).append(' ').append(content);
        }
        return this;
    }

    @Override
    public String toString() {
        return sql.toString();
    }
}
