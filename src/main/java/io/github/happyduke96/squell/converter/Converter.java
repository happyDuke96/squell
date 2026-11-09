package io.github.happyduke96.squell.converter;

/**
 * Converts an entity field of type {@code T} to and from the JDBC value {@code S} stored in the
 * column.
 *
 * <p>Implement it for a custom column type and reference it with {@code @Convert}.
 *
 * @param <T> the entity field type
 * @param <S> the JDBC value type
 */
public interface Converter<T, S> {

    S toSql(T value);

    T fromSql(S sqlValue);
}
