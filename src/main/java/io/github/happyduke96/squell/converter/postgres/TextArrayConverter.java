package io.github.happyduke96.squell.converter.postgres;

public final class TextArrayConverter extends AbstractArrayConverter<String> {

    @Override
    protected String pgType() {
        return "text[]";
    }

    @Override
    protected String serializeElement(String element) {
        return element;
    }

    @Override
    protected String deserializeElement(String literal) {
        return literal;
    }
}
