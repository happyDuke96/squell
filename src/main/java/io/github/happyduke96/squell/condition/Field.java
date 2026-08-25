package io.github.happyduke96.squell.condition;

import io.github.happyduke96.squell.converter.Converter;
import io.github.happyduke96.squell.converter.NoConverter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/// A typed reference to a table column; builds `Condition`s (`eq`, `gt`, `like`, ...).
public final class Field<T> {

    private final String name;
    private final Class<T> type;
    private final ColumnConstraints constraints;
    private final Converter<T, Object> converter;

    public Field(String name, Class<T> type) {
        this(name, type, ColumnConstraints.NONE, new NoConverter<>());
    }

    public Field(String name, Class<T> type, Converter<T, ?> converter) {
        this(name, type, ColumnConstraints.NONE, converter);
    }

    public Field(String name, Class<T> type, ColumnConstraints constraints) {
        this(name, type, constraints, new NoConverter<>());
    }

    @SuppressWarnings("unchecked")
    public Field(String name, Class<T> type, ColumnConstraints constraints, Converter<T, ?> converter) {
        this.name = name;
        this.type = type;
        this.constraints = constraints;
        this.converter = (Converter<T, Object>) converter;
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    public ColumnConstraints constraints() {
        return constraints;
    }

    public boolean generated() {
        return constraints.generated();
    }

    public boolean nonNull() {
        return constraints.nonNull();
    }

    public boolean unique() {
        return constraints.unique();
    }

    public Condition eq(T value) {
        return new Eq<>(this, value);
    }

    public Condition ne(T value) {
        return new Ne<>(this, value);
    }

    public Condition gt(T value) {
        return new Gt<>(this, value);
    }

    public Condition lt(T value) {
        return new Lt<>(this, value);
    }

    public Condition between(T low, T high) {
        return new Between<>(this, low, high);
    }

    public Condition in(Collection<T> values) {
        return new In<>(this, values);
    }

    public Condition notIn(Collection<T> values) {
        return new NotIn<>(this, values);
    }

    public Condition in(SubQuery<T> subQuery) {
        return new InSubQuery<>(this, subQuery);
    }

    public Condition notIn(SubQuery<T> subQuery) {
        return new NotInSubQuery<>(this, subQuery);
    }

    public Condition like(String pattern) {
        return new Like(this, pattern);
    }

    public Condition notLike(String pattern) {
        return new NotLike(this, pattern);
    }

    public Condition eqField(Field<T> other) {
        return new EqField<>(this, other);
    }

    public Condition isNull() {
        return new IsNull(this);
    }

    public Condition isNotNull() {
        return new IsNotNull(this);
    }

    /// `eq(value)` when `test` is true, a no-op `Condition` otherwise — for building a `WHERE`
    /// clause where a filter only applies when its value was actually supplied.
    public Condition eqIf(boolean test, T value) {
        return test ? eq(value) : new NoCondition();
    }

    public Condition neIf(boolean test, T value) {
        return test ? ne(value) : new NoCondition();
    }

    public Condition gtIf(boolean test, T value) {
        return test ? gt(value) : new NoCondition();
    }

    public Condition ltIf(boolean test, T value) {
        return test ? lt(value) : new NoCondition();
    }

    public Condition betweenIf(boolean test, T low, T high) {
        return test ? between(low, high) : new NoCondition();
    }

    public Condition inIf(boolean test, Collection<T> values) {
        return test ? in(values) : new NoCondition();
    }

    public Condition notInIf(boolean test, Collection<T> values) {
        return test ? notIn(values) : new NoCondition();
    }

    public Condition likeIf(boolean test, String pattern) {
        return test ? like(pattern) : new NoCondition();
    }

    public Condition notLikeIf(boolean test, String pattern) {
        return test ? notLike(pattern) : new NoCondition();
    }

    /// Escapes `%`, `_`, and `\` in `literal` so it matches literally in `like`/`notLike`.
    public static String escapeLike(String literal) {
        return literal.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /// A copy of this field prefixed with a table alias, e.g. `id().qualifiedBy("p")` reads as
    /// `p.id` — lets `eqField` compare the same column from two aliases of the same table.
    /// `alias` is written straight into the SQL text with no escaping — pass only a literal you
    /// write in code, never a runtime/user-supplied string.
    public Field<T> qualifiedBy(String alias) {
        return new Field<>(alias + "." + name, type, constraints, converter);
    }

    public Object toSqlValue(T value) {
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return converter.toSql(value);
    }

    @SuppressWarnings("unchecked")
    public Object readFrom(ResultSet row) throws SQLException {
        if (type.isEnum()) {
            String enumName = row.getString(name);
            return enumName == null ? null : Enum.valueOf((Class<Enum>) type, enumName);
        }
        return converter.fromSql(row.getObject(name));
    }
}
