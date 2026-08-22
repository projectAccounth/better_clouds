package net.not_thefirst.story_mode_clouds.utils.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.BooleanConfigEntry;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.ConfigEntry;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.ConfigType;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.IntegerConfigEntry;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.NumberConfigEntry;
import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.StringConfigEntry;

import java.io.IOException;
public final class ConfigEntryAdapter extends TypeAdapter<ConfigEntry> {
    @Override
    public void write(JsonWriter out, ConfigEntry entry) throws IOException {
        out.beginObject();
        out.name("type");
        out.value(entry.type().toJson());
        if (entry.description() != null) {
            out.name("description");
            out.value(entry.description());
        }
        switch (entry) {
            case StringConfigEntry e -> {
                writeDefault(out, e.defaultValue());
            }
            case NumberConfigEntry e -> {
                if (e.minimum() != null) {
                    out.name("minimum").value(e.minimum());
                }
                if (e.maximum() != null) {
                    out.name("maximum").value(e.maximum());
                }
                writeDefault(out, e.defaultValue());
            }
            case IntegerConfigEntry e -> {
                if (e.minimum() != null) {
                    out.name("minimum").value(e.minimum());
                }
                if (e.maximum() != null) {
                    out.name("maximum").value(e.maximum());
                }
                writeDefault(out, e.defaultValue());
            }
            case BooleanConfigEntry e -> {
                writeDefault(out, e.defaultValue());
            }
        }
        out.endObject();
    }

    private static void writeDefault(JsonWriter out, Object value) throws IOException {
        if (value == null) {
            return;
        }
        out.name("default");
        switch (value) {
            case String s -> out.value(s);
            case Float f -> out.value(f);
            case Long l -> out.value(l);
            case Integer i -> out.value(i);
            case Boolean b -> out.value(b);
            default -> throw new JsonParseException("Unsupported config default value: " + value.getClass());
        }
    }

    @Override
    public ConfigEntry read(JsonReader in) throws IOException {
        JsonObject obj = JsonParser.parseReader(in).getAsJsonObject();
        ConfigType type = ConfigType.fromJson(obj.get("type").getAsString());
        String description = obj.has("description") ? obj.get("description").getAsString() : null;
        return switch (type) {
            case STRING -> new StringConfigEntry(description, getStringDefault(obj));
            case NUMBER ->
                new NumberConfigEntry(description, getFloat(obj, "min"), getFloat(obj, "max"), getFloatDefault(obj));
            case INTEGER ->
                new IntegerConfigEntry(description, getLong(obj, "min"), getLong(obj, "max"), getLongDefault(obj));
            case BOOLEAN -> new BooleanConfigEntry(description, getBooleanDefault(obj));
            case OBJECT, ARRAY -> throw new JsonParseException("Unsupported config type: " + type);
        };
    }

    private static String getStringDefault(JsonObject obj) {
        JsonElement value = obj.get("default");
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static Float getFloatDefault(JsonObject obj) {
        return getFloat(obj, "default");
    }

    private static Float getFloat(JsonObject obj, String name) {
        JsonElement value = obj.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsFloat();
    }

    private static Long getLongDefault(JsonObject obj) {
        return getLong(obj, "default");
    }

    private static Long getLong(JsonObject obj, String name) {
        JsonElement value = obj.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsLong();
    }

    private static Boolean getBooleanDefault(JsonObject obj) {
        JsonElement value = obj.get("default");
        return value == null || value.isJsonNull() ? null : value.getAsBoolean();
    }
}