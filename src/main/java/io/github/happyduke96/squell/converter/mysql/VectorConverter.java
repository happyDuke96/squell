package io.github.happyduke96.squell.converter.mysql;

import io.github.happyduke96.squell.converter.Converter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class VectorConverter implements Converter<float[], byte[]> {

    @Override
    public byte[] toSql(float[] value) {
        if (value == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(value.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : value) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    @Override
    public float[] fromSql(byte[] sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(sqlValue).order(ByteOrder.LITTLE_ENDIAN);
        float[] values = new float[sqlValue.length / Float.BYTES];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.getFloat();
        }
        return values;
    }
}
