package io.github.happyduke96.squell.condition;

import io.github.happyduke96.squell.converter.Converter;
import io.github.happyduke96.squell.converter.NoConverter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * A typed reference to a table column; its comparison methods build {@link Condition}s.
 *
 * @param <T> the column's Java type
 */
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

    public Condition ge(T value) {
        return new Ge<>(this, value);
    }

    public Condition le(T value) {
        return new Le<>(this, value);
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

    public Condition ilike(String pattern) {
        return new ILike(this, pattern);
    }

    public Condition notILike(String pattern) {
        return new NotILike(this, pattern);
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

    /**
     * Returns {@code eq(value)} when {@code test} is true, otherwise a {@link NoCondition}.
     *
     * <p>Use it for a {@code WHERE} filter that applies only when its value was supplied.
     *
     * @param test whether the filter applies
     * @param value the value to compare with
     * @return the condition
     */
    public Condition eqIf(boolean test, T value) {
        return test ? eq(value) : NoCondition.INSTANCE;
    }

    public Condition neIf(boolean test, T value) {
        return test ? ne(value) : NoCondition.INSTANCE;
    }

    public Condition gtIf(boolean test, T value) {
        return test ? gt(value) : NoCondition.INSTANCE;
    }

    public Condition ltIf(boolean test, T value) {
        return test ? lt(value) : NoCondition.INSTANCE;
    }

    public Condition geIf(boolean test, T value) {
        return test ? ge(value) : NoCondition.INSTANCE;
    }

    public Condition leIf(boolean test, T value) {
        return test ? le(value) : NoCondition.INSTANCE;
    }

    public Condition betweenIf(boolean test, T low, T high) {
        return test ? between(low, high) : NoCondition.INSTANCE;
    }

    public Condition inIf(boolean test, Collection<T> values) {
        return test ? in(values) : NoCondition.INSTANCE;
    }

    public Condition notInIf(boolean test, Collection<T> values) {
        return test ? notIn(values) : NoCondition.INSTANCE;
    }

    public Condition likeIf(boolean test, String pattern) {
        return test ? like(pattern) : NoCondition.INSTANCE;
    }

    public Condition notLikeIf(boolean test, String pattern) {
        return test ? notLike(pattern) : NoCondition.INSTANCE;
    }

    public Condition ilikeIf(boolean test, String pattern) {
        return test ? ilike(pattern) : NoCondition.INSTANCE;
    }

    public Condition notILikeIf(boolean test, String pattern) {
        return test ? notILike(pattern) : NoCondition.INSTANCE;
    }

    /**
     * Escapes {@code %}, {@code _} and {@code \} so the literal matches literally in {@code like},
     * {@code notLike} and {@code ilike}.
     *
     * @param literal the text to escape
     * @return the escaped text
     */
    public static String escapeLike(String literal) {
        return literal.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Returns a copy of this field prefixed with a table alias, so {@code id().qualifiedBy("p")}
     * reads {@code p.id}.
     *
     * <p>{@code alias} is written into the SQL unescaped: pass a literal, never user input.
     *
     * @param alias the table alias
     * @return the qualified field
     */
    public Field<T> qualifiedBy(String alias) {
        return new Field<>(alias + "." + name, type, constraints, converter);
    }

    public Object toSqlValue(T value) {
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        return converter.toSql(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object readFrom(ResultSet row) throws SQLException {
        if (type.isEnum()) {
            String enumName = row.getString(name);
            return enumName == null ? null : Enum.valueOf((Class<Enum>) type, enumName);
        }
        if (type == Boolean.class) {
            Object raw = row.getObject(name);
            return switch (raw) {
                case null -> null;
                case Boolean b -> b;
                case Number n -> n.intValue() != 0;
                default -> raw;
            };
        }
        return converter.fromSql(row.getObject(name));
    }
}
