package net.not_thefirst.story_mode_clouds.config;

public sealed interface ConfigInstance<T>
        permits IntConfig, FloatConfig, StringConfig, BooleanConfig {

    T value();
    void setValue(Object value);
    void setValueFrom(ConfigInstance<?> other);

    default boolean assignableTo(ConfigInstance<?> other) {
        return other.value().getClass().isAssignableFrom(value().getClass());
    }

    static <T> ConfigInstance<?> create(T value) {
        if (value.getClass() == Integer.class || value.getClass() == int.class) {
            return new IntConfig((Integer) value);
        } else if (value.getClass() == Float.class || value.getClass() == float.class) {
            return new FloatConfig((Float) value);
        } else if (value.getClass() == String.class) {
            return new StringConfig((String) value);
        } else if (value.getClass() == Boolean.class || value.getClass() == boolean.class) {
            return new BooleanConfig((Boolean) value);
        } else {
            throw new IllegalArgumentException("Unsupported type for ConfigInstance: " + value.getClass());
        }
    }
}

final class IntConfig implements ConfigInstance<Integer> {
    private Integer value;

    public IntConfig(Integer value) {
        this.value = value;
    }

    @Override
    public Integer value() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Value is not an Integer");
        }
        this.value = (Integer) value;
    }

    @Override
    public void setValueFrom(ConfigInstance<?> other) {
        if (other instanceof IntConfig) {
            this.value = ((IntConfig) other).value();
        }
    }

}

final class FloatConfig implements ConfigInstance<Float> {
    private Float value;

    public FloatConfig(Float value) {
        this.value = value;
    }

    @Override
    public Float value() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof Float)) {
            throw new IllegalArgumentException("Value is not a Float");
        }
        this.value = (Float) value;
    }

    @Override
    public void setValueFrom(ConfigInstance<?> other) {
        if (other instanceof FloatConfig) {
            this.value = ((FloatConfig) other).value();
        }
    }
}

final class StringConfig implements ConfigInstance<String> {
    private String value;

    public StringConfig(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Value is not a String");
        }
        this.value = (String) value;
    }

    @Override
    public void setValueFrom(ConfigInstance<?> other) {
        if (other instanceof StringConfig) {
            this.value = ((StringConfig) other).value();
        }
    }
}

final class BooleanConfig implements ConfigInstance<Boolean> {
    private Boolean value;

    public BooleanConfig(Boolean value) {
        this.value = value;
    }

    @Override
    public Boolean value() {
        return value;
    }

    @Override
    public void setValue(Object value) {
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("Value is not a Boolean");
        }
        this.value = (Boolean) value;
    }

    @Override
    public void setValueFrom(ConfigInstance<?> other) {
        if (other instanceof BooleanConfig) {
            this.value = ((BooleanConfig) other).value();
        }
    }
}