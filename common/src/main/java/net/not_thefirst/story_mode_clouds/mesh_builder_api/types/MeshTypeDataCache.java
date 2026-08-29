package net.not_thefirst.story_mode_clouds.mesh_builder_api.types;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeshTypeDataCache {
    private static final Set<MeshTypeData> dataList = new HashSet<>();

    private MeshTypeDataCache() {}

    public static void addData(MeshTypeData data) {
        dataList.add(data);
    }

    public static MeshTypeData get(String name) {
        for (MeshTypeData data : dataList) {
            if (data.name().equals(name)) {
                return data;
            }
        }
        return null;
    }

    public static List<MeshTypeData> getData() {
        return dataList.stream().toList();
    }

    public static void flush() {
        dataList.clear();
    }
}
