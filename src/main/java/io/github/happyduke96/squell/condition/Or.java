package io.github.happyduke96.squell.condition;

public final class Or extends BinaryCondition {

    Or(Condition left, Condition right) {
        super(left, right);
    }

    @Override
    String operator() {
        return "OR";
    }
}
