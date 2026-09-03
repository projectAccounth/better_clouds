package net.not_thefirst.story_mode_clouds.utils.json;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.Dimension;

public final class CloudsConfigurationAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() != CloudsConfiguration.class) {
            return null;
        }

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonElement element = JsonParser.parseReader(in);
                migrateLegacyLayers(element);
                return delegate.fromJsonTree(element);
            }
        };
    }

    private static void migrateLegacyLayers(JsonElement element) {
        if (!element.isJsonObject()) {
            return;
        }

        JsonObject config = element.getAsJsonObject();
        JsonObject dimensionLayers = config.has("DIMENSION_LAYERS")
            && config.get("DIMENSION_LAYERS").isJsonObject()
            ? config.getAsJsonObject("DIMENSION_LAYERS")
            : new JsonObject();

        migrateLayer(config, dimensionLayers, "LAYERS", Dimension.OVERWORLD);
        migrateLayer(config, dimensionLayers, "NETHER_LAYERS", Dimension.NETHER);
        migrateLayer(config, dimensionLayers, "END_LAYERS", Dimension.END);

        if (dimensionLayers.size() > 0) {
            config.add("DIMENSION_LAYERS", dimensionLayers);
        }
    }

    private static void migrateLayer(
        JsonObject config,
        JsonObject dimensionLayers,
        String legacyName,
        Dimension dimension
    ) {
        if (!config.has(legacyName) || dimensionLayers.has(dimension.name())) {
            return;
        }

        JsonElement legacyHolder = config.get(legacyName);
        if (legacyHolder != null && !legacyHolder.isJsonNull()) {
            dimensionLayers.add(dimension.name(), legacyHolder);
        }
    }
}