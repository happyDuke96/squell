package io.github.happyduke96.squell.condition;

import java.util.Collections;
import java.util.List;

public final class Like implements Condition {

    private final Field<?> field;
    private final String pattern;

    Like(Field<?> field, String pattern) {
        this.field = field;
        this.pattern = pattern;
    }

    @Override
    public String sql() {
        return field.name() + " LIKE ?";
    }

    @Override
    public List<Object> values() {
        return Collections.singletonList(pattern);
    }
}
