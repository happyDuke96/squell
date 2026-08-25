package io.github.happyduke96.squell.condition;

public final class And extends BinaryCondition {

    And(Condition left, Condition right) {
        super(left, right);
    }

    @Override
    String operator() {
        return "AND";
    }
}
