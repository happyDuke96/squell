package io.github.happyduke96.squell.condition;

import java.util.List;

/// Null Object for `Condition` — empty SQL, no values; `and`/`or` with it just return the other
/// side. Returned by `Field`'s `xIf(false, ...)` methods and used as the seed in `ConditionBuilder`.
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
