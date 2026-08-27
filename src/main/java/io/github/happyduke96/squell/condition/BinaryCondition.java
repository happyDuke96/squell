package io.github.happyduke96.squell.condition;

import java.util.ArrayList;
import java.util.List;

public sealed abstract class BinaryCondition implements Condition permits And, Or {

    private final Condition left;
    private final Condition right;

    BinaryCondition(Condition left, Condition right) {
        this.left = left;
        this.right = right;
    }

    abstract String operator();

    @Override
    public final String sql() {
        return "(" + left.sql() + " " + operator() + " " + right.sql() + ")";
    }

    @Override
    public final List<Object> values() {
        List<Object> combined = new ArrayList<>(left.values());
        combined.addAll(right.values());
        return combined;
    }
}
