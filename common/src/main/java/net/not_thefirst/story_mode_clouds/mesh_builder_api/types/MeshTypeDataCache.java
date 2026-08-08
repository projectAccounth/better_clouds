package net.not_thefirst.story_mode_clouds.mesh_builder_api.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshTypeDataCache {
    private static final Map<String, MeshTypeData> dataList = new HashMap<>();

    private MeshTypeDataCache() {}

    public static void addData(MeshTypeData data) {
        dataList.put(data.name(), data);
    }

    public static MeshTypeData get(String name) {
        return dataList.get(name);
    }

    public static List<MeshTypeData> getData() {
        return dataList.values().stream().toList();
    }

    public static void flush() {
        dataList.clear();
    }
}
