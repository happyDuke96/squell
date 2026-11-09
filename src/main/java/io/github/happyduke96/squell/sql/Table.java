package io.github.happyduke96.squell.sql;

import io.github.happyduke96.squell.condition.Field;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A generated mapping between an {@code @Entity} and its table.
 *
 * <p>It knows the table's fields, builds an entity from a {@code ResultSet}, and writes every field
 * except generated ones on insert. Being a {@link RowMapper}, it maps whole rows of raw SQL too.
 *
 * @param <T> the entity type
 */
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

    /**
     * Returns this table under an alias.
     *
     * <p>{@code alias} is written into the SQL unescaped: pass a literal such as
     * {@code posts.as("p")}, never user input.
     *
     * @param alias the table alias
     * @return the aliased table
     */
    default AliasedTable<T> as(String alias) {
        return new AliasedTable<>(this, alias);
    }
}
