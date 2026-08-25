package io.github.happyduke96.squell.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/// Accumulates optional filters into one `Condition` — `add(value, toCondition)` skips null/empty
/// values instead of requiring the caller to branch before adding.
public final class ConditionBuilder {

    private final List<Condition> conditions = new ArrayList<>();

    public ConditionBuilder add(Condition condition) {
        conditions.add(condition);
        return this;
    }

    public <T> ConditionBuilder add(T value, Function<T, Condition> toCondition) {
        return isPresent(value) ? add(toCondition.apply(value)) : this;
    }

    private static boolean isPresent(Object value) {
        return switch (value) {
            case null -> false;
            case String s -> !s.isEmpty();
            case Collection<?> c -> !c.isEmpty();
            default -> true;
        };
    }

    public Condition buildAnd() {
        return conditions.stream().reduce(new NoCondition(), Condition::and);
    }

    public Condition buildOr() {
        return conditions.stream().reduce(new NoCondition(), Condition::or);
    }
}
