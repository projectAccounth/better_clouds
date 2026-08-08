package net.not_thefirst.story_mode_clouds.utils.json;

import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigValueAdapter extends TypeAdapter<Object> {
    
    @Override
    public void write(JsonWriter out, Object value) throws IOException {
        throw new UnsupportedOperationException("Serialization not implemented.");
    }

    @Override
    public Object read(JsonReader in) throws IOException {
        switch (in.peek()) {
            case STRING: 
                return in.nextString();
            case NUMBER:
                String numStr = in.nextString();
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    return Double.parseDouble(numStr);
                }
                return Integer.parseInt(numStr);
            case BOOLEAN: 
                return in.nextBoolean();
            case NULL: 
                in.nextNull(); 
                return null;
            case BEGIN_ARRAY:
                List<Object> list = new ArrayList<>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(read(in));
                }
                in.endArray();
                return list;
            case BEGIN_OBJECT:
                Map<String, Object> map = new LinkedTreeMap<>();
                in.beginObject();
                while (in.hasNext()) {
                    map.put(in.nextName(), read(in));
                }
                in.endObject();
                return map;
            default:
                in.skipValue();
                return null;
        }
    }
}