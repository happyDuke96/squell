package io.github.happyduke96.squell.converter;

/// Converts an entity field of type `T` to/from the JDBC value `S` stored in the column. Implement
/// this for a custom column type and reference it with `@Convert`.
public interface Converter<T, S> {

    S toSql(T value);

    T fromSql(S sqlValue);
}
