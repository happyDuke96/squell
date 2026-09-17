package io.github.happyduke96.squell.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// Accumulates optional filters into one `Condition` — `add(value, toCondition)` skips null/empty
/// values instead of requiring the caller to branch before adding.
public final class ConditionBuilder {

    private final List<Condition> conditions;

    public ConditionBuilder() {
        this(List.of());
    }

    private ConditionBuilder(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public ConditionBuilder add(Condition condition) {
        List<Condition> next = new ArrayList<>(conditions);
        next.add(condition);
        return new ConditionBuilder(next);
    }

    public <T> ConditionBuilder add(T value, Function<T, Condition> toCondition) {
        return isPresent(value) ? add(toCondition.apply(value)) : this;
    }

    private static boolean isPresent(Object value) {
        return switch (value) {
            case null -> false;
            case String s -> !s.isBlank();
            case Collection<?> c -> !c.isEmpty();
            case Map<?, ?> m -> !m.isEmpty();
            case Number n -> n.doubleValue() > 0;
            case Boolean ignored -> true;
            default -> true;
        };
    }

    public Condition buildAnd() {
        return conditions.stream().reduce(NoCondition.INSTANCE, Condition::and);
    }

    public Condition buildOr() {
        return conditions.stream().reduce(NoCondition.INSTANCE, Condition::or);
    }
}
