package net.not_thefirst.story_mode_clouds.renderer.mesh_builders.mesh;

import com.mojang.blaze3d.vertex.BufferBuilder;

interface VertexEmitter {
    void emit(BufferBuilder bb, int i, float ox, float oy, float oz);
}