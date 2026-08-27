package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Field;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/// A generated mapping between an `@Entity` record and its table: its fields, how to build a row
/// from a `ResultSet`, and which fields are actually written on insert (generated ones are not).
public interface Table<T> extends RowMapper<T> {

    String name();

    List<Field<?>> fields();

    T fromRow(ResultSet row) throws SQLException;

    List<Object> values(T entity);

    default List<Field<?>> insertableFields() {
        List<Field<?>> insertable = new ArrayList<>();
        for (Field<?> field : fields()) {
            if (!field.generated()) {
                insertable.add(field);
            }
        }
        return insertable;
    }

    default List<Object> insertableValues(T entity) {
        List<Field<?>> allFields = fields();
        List<Object> allValues = values(entity);
        List<Object> insertable = new ArrayList<>();
        for (int i = 0; i < allFields.size(); i++) {
            if (!allFields.get(i).generated()) {
                insertable.add(allValues.get(i));
            }
        }
        return insertable;
    }

    @Override
    default T map(ResultSet row) throws SQLException {
        return fromRow(row);
    }

    /// Aliases this table. `alias` is written straight into the SQL text with no escaping — pass
    /// only a literal you write in code (`posts.as("p")`), never a runtime/user-supplied string.
    default AliasedTable<T> as(String alias) {
        return new AliasedTable<>(this, alias);
    }
}
