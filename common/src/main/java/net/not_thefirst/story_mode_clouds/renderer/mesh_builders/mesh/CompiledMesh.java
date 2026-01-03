package net.not_thefirst.story_mode_clouds.renderer.mesh_builders.mesh;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

// for the scripting engine
public final class CompiledMesh {
    final float[] positions;
    final float[] normals;
    final float[] uvs;
    final int[] colors;
    final int vertexCount;

    final VertexFormat format;
    final VertexFormat.Mode mode;
    final VertexEmitter emitter;

    CompiledMesh(
        float[] positions,
        float[] normals,
        float[] uvs,
        int[] colors,
        int vertexCount,
        VertexFormat format,
        VertexFormat.Mode mode
    ) {
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.colors = colors;
        this.vertexCount = vertexCount;
        this.format = format;
        this.mode = mode;
        this.emitter = buildEmitter(format);
    }

    void put(BufferBuilder bb, float ox, float oy, float oz) {
        for (int i = 0; i < vertexCount; i++) {
            emitter.emit(bb, i, ox, oy, oz);
        }
    }


    private VertexEmitter buildEmitter(VertexFormat format) {
        boolean hasUV = format.contains(VertexFormatElement.UV);
        boolean hasNormal = format.contains(VertexFormatElement.NORMAL);
        boolean hasColor = format.contains(VertexFormatElement.COLOR);

        return (bb, i, ox, oy, oz) -> {
            int p = i * 3;

            bb.addVertex(
                positions[p]     + ox,
                positions[p + 1] + oy,
                positions[p + 2] + oz
            );

            if (hasColor)
                bb.setColor(colors[i]);

            if (hasUV)
                bb.setUv(uvs[i * 2], uvs[i * 2 + 1]);

            if (hasNormal)
                bb.setNormal(
                    normals[p],
                    normals[p + 1],
                    normals[p + 2]
                );
        };
    }
}
