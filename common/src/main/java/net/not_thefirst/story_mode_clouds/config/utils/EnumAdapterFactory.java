package net.not_thefirst.story_mode_clouds.config.utils;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class EnumAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<T> rawType = (Class<T>) type.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }

        final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null; 
                }

                String valueStr = in.nextString();
                try {
                    return (T) Enum.valueOf((Class<Enum>) rawType, valueStr);
                } catch (IllegalArgumentException e) {
                    T[] constants = rawType.getEnumConstants();
                    if (constants != null && constants.length > 0) {
                        return constants[0]; 
                    }
                    return null;
                }
            }
        };
    }
}
