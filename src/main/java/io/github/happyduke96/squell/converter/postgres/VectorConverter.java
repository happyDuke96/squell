package io.github.happyduke96.squell.converter.postgres;

public final class VectorConverter extends AbstractVectorConverter<float[]> {

    @Override
    protected String serialize(float[] value) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < value.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(value[i]);
        }
        return text.append(']').toString();
    }

    @Override
    protected float[] deserialize(String vectorText) {
        String trimmed = vectorText.substring(1, vectorText.length() - 1);
        if (trimmed.isEmpty()) {
            return new float[0];
        }
        String[] parts = trimmed.split(",");
        float[] values = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Float.parseFloat(parts[i]);
        }
        return values;
    }
}
