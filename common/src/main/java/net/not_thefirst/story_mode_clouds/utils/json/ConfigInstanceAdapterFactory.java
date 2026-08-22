package net.not_thefirst.story_mode_clouds.utils.json;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.not_thefirst.story_mode_clouds.config.ConfigInstance;

public class ConfigInstanceAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!ConfigInstance.class.isAssignableFrom(type.getRawType())) {
            return null;
        }
        return (TypeAdapter<T>) new ConfigInstanceAdapter();
    }

    public static class ConfigInstanceAdapter extends TypeAdapter<ConfigInstance> {

        @Override
        public void write(JsonWriter out, ConfigInstance value) {
            Object underlying = value.value();

            if (underlying instanceof String) {
                try {
                    out.value((String) underlying);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (underlying instanceof Number) {
                try {
                    out.value((Number) underlying);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (underlying instanceof Boolean) {
                try {
                    out.value((Boolean) underlying);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (underlying == null) {
                try {
                    out.nullValue();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException("Unsupported type for ConfigInstance: " + underlying.getClass());
            }
        }

        @Override
        public ConfigInstance read(JsonReader in) throws IOException {
            switch (in.peek()) {
                case STRING:
                    return ConfigInstance.create(in.nextString());
                case NUMBER:
                    String numStr = in.nextString();
                    if (numStr.contains(".")) {
                        return ConfigInstance.create(Float.parseFloat(numStr));
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

}