package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;

class B3DConversions {
    private B3DConversions() {}

    public static DepthTestFunction toCompareOp(DepthTestState depthTestState) {
        DepthTestFunction compareOp = null;
        if (depthTestState == DepthTestState.LEQUAL) {
            compareOp = DepthTestFunction.LEQUAL_DEPTH_TEST;
        } 
        else if (depthTestState == DepthTestState.LESS) {
            compareOp = DepthTestFunction.LESS_DEPTH_TEST;
        }
        else if (depthTestState == DepthTestState.GREATER) {
            compareOp = DepthTestFunction.GREATER_DEPTH_TEST;
        }
        else if (depthTestState == DepthTestState.EQUALS) {
            compareOp = DepthTestFunction.EQUAL_DEPTH_TEST;
        }
        else if (depthTestState == DepthTestState.NEVER) {
            compareOp = DepthTestFunction.NO_DEPTH_TEST;
        }
        return compareOp;
    }

    public static com.mojang.blaze3d.vertex.VertexFormat toVanillaVertexFormat(VertexFormat vertexFormat) {
        com.mojang.blaze3d.vertex.VertexFormat format = null;
        if (vertexFormat == VertexFormat.POSITION_COLOR_TEX) {
            format = DefaultVertexFormat.POSITION_TEX_COLOR;
        }
        else if (vertexFormat == VertexFormat.POSITION_COLOR) {
            format = DefaultVertexFormat.POSITION_COLOR;
        }
        else if (vertexFormat == VertexFormat.POSITION_COLOR_NORMAL_TEX) {
            format = DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
        }
        else if (vertexFormat == VertexFormat.POSITION_COLOR_NORMAL) {
            format = DefaultVertexFormat.POSITION_COLOR_NORMAL;
        }
        else if (vertexFormat == VertexFormat.POSITION_TEX) {
            format = DefaultVertexFormat.POSITION_TEX;
        }
        return format;
    }

    public static com.mojang.blaze3d.vertex.VertexFormat.Mode toPrimitiveTopology(GLPrimitive primitive) {
        switch (primitive) {
            case TRIANGLES: return com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES;
            case LINES: return com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES;
            case QUADS: return com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS;
            default: return null;
        }
    }
}
