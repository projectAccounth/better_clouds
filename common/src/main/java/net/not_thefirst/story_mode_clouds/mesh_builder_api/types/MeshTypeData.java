package net.not_thefirst.story_mode_clouds.mesh_builder_api.types;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public record MeshTypeData(
        String name,
        @SerializedName("render") RenderOptions renderOptions,
        @SerializedName("generator") GeneratorConfig generatorConfig,
        Map<String, ConfigEntry> config) {

    public ConfigEntry getEntryDefinition(String key) {
        return config.get(key);
    }

    public enum ConfigType {
        STRING,
        NUMBER,
        INTEGER,
        BOOLEAN,
        OBJECT,
        ARRAY;

        public static ConfigType fromJson(String value) {
            return valueOf(value.toUpperCase());
        }

        public String toJson() {
            return name().toLowerCase();
        }
    }
    public sealed interface ConfigEntry
            permits StringConfigEntry,
            NumberConfigEntry,
            IntegerConfigEntry,
            BooleanConfigEntry {
        ConfigType type();

        String description();

        Object defaultValue();

        default boolean isString() {
            return type() == ConfigType.STRING;
        }

        default boolean isFloat() {
            return type() == ConfigType.NUMBER;
        }

        default boolean isInteger() {
            return type() == ConfigType.INTEGER;
        }

        default boolean isBoolean() {
            return type() == ConfigType.BOOLEAN;
        }

        default boolean isOfNumericType() {
            return isFloat() || isInteger();
        }

        /** Whether a runtime value can be assigned to this config entry. */
        boolean isObjectValid(Object value);
    }

    public record StringConfigEntry(String description, String defaultValue) implements ConfigEntry {

        public StringConfigEntry {
            if (defaultValue == null) {
                defaultValue = "";
            }
        }

        @Override
        public ConfigType type() {
            return ConfigType.STRING;
        }

        @Override
        public boolean isObjectValid(Object value) {
            return value instanceof String;
        }
    }

    public record NumberConfigEntry(String description, Float minimum, Float maximum, Float defaultValue)
            implements ConfigEntry {

        public NumberConfigEntry {
            if (minimum == null) {
                minimum = Float.NEGATIVE_INFINITY;
            }
            if (maximum == null) {
                maximum = Float.POSITIVE_INFINITY;
            }
            if (defaultValue == null) {
                defaultValue = 0.0f;
            }
        }

        @Override
        public ConfigType type() {
            return ConfigType.NUMBER;
        }

        @Override
        public boolean isObjectValid(Object value) {
            return value instanceof Float;
        }
    }

    public record IntegerConfigEntry(String description, Long minimum, Long maximum, Long defaultValue)
            implements ConfigEntry {

        public IntegerConfigEntry {
            if (minimum == null) {
                minimum = Long.valueOf(Integer.MIN_VALUE);
            }
            if (maximum == null) {
                maximum = Long.valueOf(Integer.MAX_VALUE);
            }
            if (defaultValue == null) {
                defaultValue = 0L;
            }
        }
            
        @Override
        public ConfigType type() {
            return ConfigType.INTEGER;
        }

        @Override
        public boolean isObjectValid(Object value) {
            return value instanceof Integer || value instanceof Long;
        }
    }

    public record BooleanConfigEntry(String description, Boolean defaultValue) implements ConfigEntry {
        
        public BooleanConfigEntry {
            if (defaultValue == null) {
                defaultValue = false;
            }
        }

        @Override
        public ConfigType type() {
            return ConfigType.BOOLEAN;
        }

        @Override
        public boolean isObjectValid(Object value) {
            return value instanceof Boolean;
        }
    }

    public static record RenderOptions(
            boolean depth,
            @SerializedName("outline_available") boolean outlineAvailable) {
    }

    public static record GeneratorConfig(
            GeneratorType type,
            @SerializedName("base") String baseScriptName,
            @SerializedName("outline") String outlineScriptName) {
    }

    public enum GeneratorType {
        @SerializedName("native")
        NATIVE,
        @SerializedName("custom")
        CUSTOM
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        MeshTypeData that = (MeshTypeData) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
