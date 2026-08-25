package io.github.happyduke96.squell.condition;

public final class Aggregate<T> {

    private final String expression;

    private Aggregate(String expression) {
        this.expression = expression;
    }

    public static Aggregate<Long> count() {
        return new Aggregate<>("COUNT(*)");
    }

    public static <N extends Number> Aggregate<Double> sum(Field<N> field) {
        return new Aggregate<>("SUM(" + field.name() + ")");
    }

    public static <N extends Number> Aggregate<Double> avg(Field<N> field) {
        return new Aggregate<>("AVG(" + field.name() + ")");
    }

    public static <V> Aggregate<V> min(Field<V> field) {
        return new Aggregate<>("MIN(" + field.name() + ")");
    }

    public static <V> Aggregate<V> max(Field<V> field) {
        return new Aggregate<>("MAX(" + field.name() + ")");
    }

    public Condition eq(T value) {
        return new AggregateEq<>(expression, value);
    }

    public Condition ne(T value) {
        return new AggregateNe<>(expression, value);
    }

    public Condition gt(T value) {
        return new AggregateGt<>(expression, value);
    }

    public Condition ge(T value) {
        return new AggregateGe<>(expression, value);
    }

    public Condition lt(T value) {
        return new AggregateLt<>(expression, value);
    }

    public Condition le(T value) {
        return new AggregateLe<>(expression, value);
    }
}
