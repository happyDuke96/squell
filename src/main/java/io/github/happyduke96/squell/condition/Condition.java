package io.github.happyduke96.squell.condition;

import java.util.List;

/**
 * A {@code WHERE} or {@code HAVING} predicate: its SQL text and the positional values it binds.
 *
 * <p>Built from the comparison methods of {@link Field} ({@code eq}, {@code gt}, {@code like}...)
 * and composed with {@code and}, {@code or} and {@code negate}.
 */
public sealed interface Condition permits
        Eq, Ne, Gt, Lt, Ge, Le,
        Between, In, NotIn,
        Like, NotLike, ILike, NotILike, InSubQuery,
        NotInSubQuery, EqField, IsNull,
        IsNotNull, AggregateEq, AggregateNe,
        AggregateGt, AggregateGe, AggregateLt, AggregateLe,
        BinaryCondition, Not, NoCondition
{

    String sql();

    List<Object> values();

    default Condition and(Condition other) {
        return other instanceof NoCondition ? this : new And(this, other);
    }

    default Condition or(Condition other) {
        return other instanceof NoCondition ? this : new Or(this, other);
    }

    default Condition negate() {
        return new Not(this);
    }
}
