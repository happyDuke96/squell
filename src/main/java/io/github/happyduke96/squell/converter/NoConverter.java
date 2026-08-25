package io.github.happyduke96.squell.converter;

public final class NoConverter<T> implements Converter<T, Object> {

    @Override
    public Object toSql(T value) {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T fromSql(Object sqlValue) {
        return (T) sqlValue;
    }
}
