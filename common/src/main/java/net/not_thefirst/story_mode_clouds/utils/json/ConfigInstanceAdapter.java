package net.not_thefirst.story_mode_clouds.utils.json;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.not_thefirst.story_mode_clouds.config.ConfigInstance;

public class ConfigInstanceAdapter extends TypeAdapter<ConfigInstance<?>> {
    @Override
    public void write(JsonWriter out, ConfigInstance<?> value) {
        
    }

    @Override
    public ConfigInstance<?> read(JsonReader in) throws IOException {
        switch (in.peek()) {
            case STRING:
                return ConfigInstance.create(in.nextString());
            case NUMBER:
                String numStr = in.nextString();
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    return ConfigInstance.create(Double.parseDouble(numStr));
                }
                return ConfigInstance.create(Integer.parseInt(numStr));
            case BOOLEAN:
                return ConfigInstance.create(in.nextBoolean());
            case NULL:
                in.nextNull();
                return null;
            default:
                throw new IllegalArgumentException("Unsupported JSON value type for ConfigInstance: " + in.peek());
        }
    }
    
}
