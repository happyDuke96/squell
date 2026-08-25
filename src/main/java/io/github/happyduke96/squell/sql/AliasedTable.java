package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Field;

/// A `Table` under an alias, for self-joins or joins on the same table twice — from `Table#as`.
public final class AliasedTable<T> {

    private final Table<T> table;
    private final String alias;

    public AliasedTable(Table<T> table, String alias) {
        this.table = table;
        this.alias = alias;
    }

    public String alias() {
        return alias;
    }

    public String tableName() {
        return table.name();
    }

    public <V> Field<V> field(Field<V> field) {
        return field.qualifiedBy(alias);
    }

    public RowMapper<T> mapper() {
        return table;
    }
}
