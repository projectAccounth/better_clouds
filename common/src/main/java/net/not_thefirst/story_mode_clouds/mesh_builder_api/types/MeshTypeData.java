package net.not_thefirst.story_mode_clouds.mesh_builder_api.types;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public record MeshTypeData(
    String name,
    @SerializedName("render") RenderOptions renderOptions,
    @SerializedName("generator") GeneratorConfig generatorConfig,
    Map<String, ConfigEntry> config
) {

    public ConfigEntry getEntryDefinition(String key) {
        return config.get(key);
    }

    public static record ConfigEntry(
        ConfigType type,
        String description,

        // for numeric types
        Double minimum,
        Double maximum,
          
        @SerializedName("default") 
        Object defaultValue
    ) {
        public boolean isObjectValid(Object value) {
            if (value == null) return false;
            switch (type) {
                case STRING -> { return value instanceof String; }
                case NUMBER -> { return value instanceof Double || value instanceof Float; }
                case INTEGER -> { return value instanceof Integer || value instanceof Long; }
                case BOOLEAN -> { return value instanceof Boolean; }
                case OBJECT -> { return false; } // custom objects are not yet supported
                case ARRAY -> { return false; } // arrays are not yet supported
                default -> throw new IllegalArgumentException("Unexpected value: " + type);
            }
        }
        
        public boolean isDouble() {
            return type == ConfigType.NUMBER;
        }

        public boolean isInteger() {
            return type == ConfigType.INTEGER;
        }

        public boolean isString() {
            return type == ConfigType.STRING;
        }

        public boolean isBoolean() {
            return type == ConfigType.BOOLEAN;
        }

        public boolean isNumber() {
            return isDouble() || isInteger();
        }
    }

    public static record RenderOptions(
        boolean depth,
        @SerializedName("outline_available") boolean outlineAvailable
    ) {}
    public static record GeneratorConfig(
        GeneratorType type, 
        @SerializedName("base") String baseScriptName, 
        @SerializedName("outline") String outlineScriptName
    ) {}

    public enum GeneratorType {
        @SerializedName("native") NATIVE, 
        @SerializedName("custom") CUSTOM
    }

    public enum ConfigType {
        @SerializedName("string") STRING,
        @SerializedName("number") NUMBER,
        @SerializedName("integer") INTEGER,
        @SerializedName("boolean") BOOLEAN,
        @SerializedName("object") OBJECT,
        @SerializedName("array") ARRAY
    }
}
