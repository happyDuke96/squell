package io.github.happyduke96.squell.support;

import java.util.regex.Pattern;

public final class PrettySqlFormatter implements SqlFormatter {

    private static final Pattern CLAUSE = Pattern.compile(
            "\\b(LEFT\\s+JOIN|INNER\\s+JOIN|RIGHT\\s+JOIN|GROUP\\s+BY|ORDER\\s+BY|INSERT\\s+INTO|DELETE\\s+FROM"
                    + "|SELECT|FROM|WHERE|HAVING|LIMIT|OFFSET|JOIN|SET|VALUES|RETURNING|UPDATE|ON)\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String format(String sql) {
        return CLAUSE.matcher(sql).replaceAll("\n$1").strip();
    }
}
