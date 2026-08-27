package io.github.happyduke96.squell.condition;

public record ColumnConstraints(boolean generated, boolean nonNull, boolean unique) {

    public static final ColumnConstraints NONE = new ColumnConstraints(false, false, false);
}
