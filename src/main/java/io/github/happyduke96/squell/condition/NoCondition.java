package io.github.happyduke96.squell.condition;

import java.util.List;

/**
 * Null Object for {@link Condition}: empty SQL and no values.
 *
 * <p>{@code and} and {@code or} with it return the other side. It is what the
 * {@code xIf(false, ...)} methods of {@link Field} return.
 */
public final class NoCondition implements Condition {

    public static final NoCondition INSTANCE = new NoCondition();

    @Override
    public String sql() {
        return "";
    }

    @Override
    public List<Object> values() {
        return List.of();
    }

    @Override
    public Condition and(Condition other) {
        return other;
    }

    @Override
    public Condition or(Condition other) {
        return other;
    }
}
