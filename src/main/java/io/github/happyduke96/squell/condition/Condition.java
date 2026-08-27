package io.github.happyduke96.squell.condition;

import java.util.List;

/// A `WHERE`/`HAVING` predicate — its SQL text plus the positional values it binds. Built via
/// `Field`'s comparison methods (`eq`, `gt`, `like`, ...), then composed with `and`/`or`/`negate`.
public sealed interface Condition permits
        Eq, Ne, Gt, Lt,
        Between, In, NotIn,
        Like, NotLike, InSubQuery,
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
