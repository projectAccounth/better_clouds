package net.not_thefirst.story_mode_clouds.renderer.mesh_builders.mesh;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.not_thefirst.story_mode_clouds.renderer.utils.VertexBuilder;

// for the scripting engine
public final class CompiledMesh {
    final float[] positions;
    final float[] normals;
    final float[] uvs;
    final int[] colors;
    final int vertexCount;

    final VertexFormat format;
    final int mode;
    final VertexEmitter emitter;

    CompiledMesh(
        float[] positions,
        float[] normals,
        float[] uvs,
        int[] colors,
        int vertexCount,
        VertexFormat format,
        int mode
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
        ImmutableList<VertexFormatElement> elements = format.getElements();
        boolean hasUV = elements.contains(DefaultVertexFormat.ELEMENT_UV0);
        boolean hasNormal = elements.contains(DefaultVertexFormat.ELEMENT_NORMAL);
        boolean hasColor = elements.contains(DefaultVertexFormat.ELEMENT_COLOR);

        return (bb, i, ox, oy, oz) -> {
            int p = i * 3;

            bb.vertex(
                positions[p]     + ox,
                positions[p + 1] + oy,
                positions[p + 2] + oz
            );

            if (hasColor) {
                int[] decomposed = VertexBuilder.decomposeARGB(colors[i]);
                bb.color(decomposed[1], decomposed[2], decomposed[3], decomposed[0]);
            }
                
            if (hasUV)
                bb.uv(uvs[i * 2], uvs[i * 2 + 1]);

            if (hasNormal)
                bb.normal(
                    normals[p],
                    normals[p + 1],
                    normals[p + 2]
                );
            bb.endVertex();
        };
    }
}
