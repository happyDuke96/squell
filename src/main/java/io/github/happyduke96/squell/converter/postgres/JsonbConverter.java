package io.github.happyduke96.squell.converter.postgres;

public final class JsonbConverter extends AbstractJsonbConverter<String> {

    @Override
    protected String serialize(String value) {
        return value;
    }

    @Override
    protected String deserialize(String json) {
        return json;
    }
}
