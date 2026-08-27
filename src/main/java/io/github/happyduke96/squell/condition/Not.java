package io.github.happyduke96.squell.condition;

import java.util.List;

public final class Not implements Condition {

    private final Condition condition;

    Not(Condition condition) {
        this.condition = condition;
    }

    @Override
    public String sql() {
        return "NOT (" + condition.sql() + ")";
    }

    @Override
    public List<Object> values() {
        return condition.values();
    }
}
