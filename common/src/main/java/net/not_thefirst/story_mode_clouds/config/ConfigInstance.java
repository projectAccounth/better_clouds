package net.not_thefirst.story_mode_clouds.config;

public sealed interface ConfigInstance<T>
    permits IntConfig, DoubleConfig, StringConfig, BooleanConfig {

    T value();

    default boolean assignableTo(ConfigInstance<?> other) {
        return other.value().getClass().isAssignableFrom(value().getClass());
    }

    static <T> ConfigInstance<?> create(T value) {
        if (value.getClass() == Integer.class || value.getClass() == int.class) {
            return new IntConfig((Integer) value);
        } else if (value.getClass() == Double.class || value.getClass() == double.class) {
            return new DoubleConfig((Double) value);
        } else if (value.getClass() == String.class) {
            return new StringConfig((String) value);
        } else if (value.getClass() == Boolean.class || value.getClass() == boolean.class) {
            return new BooleanConfig((Boolean) value);
        } else {
            throw new IllegalArgumentException("Unsupported type for ConfigInstance: " + value.getClass());
        }
    }
}

record IntConfig(
    Integer value
) implements ConfigInstance<Integer> {}

record DoubleConfig(
    Double value
) implements ConfigInstance<Double> {}

record StringConfig(
    String value
) implements ConfigInstance<String> {}

record BooleanConfig(
    Boolean value
) implements ConfigInstance<Boolean> {}