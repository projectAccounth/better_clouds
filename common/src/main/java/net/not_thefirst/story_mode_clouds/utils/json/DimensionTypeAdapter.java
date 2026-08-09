package net.not_thefirst.story_mode_clouds.utils.json;

import java.io.IOException;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.Dimension;

public final class DimensionTypeAdapter extends TypeAdapter<Dimension> {

    @Override
    public void write(JsonWriter out, Dimension value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.value(value.name());
    }

    @Override
    public Dimension read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String name = in.nextString();

        try {
            return Dimension.getOrRegister(name);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(
                    "Unknown Dimension value: " + name,
                    e
            );
        }
    }
}