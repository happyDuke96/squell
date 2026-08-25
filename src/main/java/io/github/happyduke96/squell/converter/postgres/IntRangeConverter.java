package io.github.happyduke96.squell.converter.postgres;

public final class IntRangeConverter extends AbstractRangeConverter<Integer> {

    @Override
    protected String pgType() {
        return "int4range";
    }

    @Override
    protected String serializeBound(Integer bound) {
        return bound.toString();
    }

    @Override
    protected Integer deserializeBound(String literal) {
        return Integer.valueOf(literal);
    }
}
