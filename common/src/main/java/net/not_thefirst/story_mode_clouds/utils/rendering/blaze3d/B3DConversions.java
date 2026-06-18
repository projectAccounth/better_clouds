package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.not_thefirst.lib.gl_render_system.state.BlendState;
import net.not_thefirst.lib.gl_render_system.state.DepthTestState;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;

class B3DConversions {
    private B3DConversions() {}

    public static CompareOp toCompareOp(DepthTestState depthTestState) {
        CompareOp compareOp = null;
        if (depthTestState == DepthTestState.LEQUAL) {
            compareOp = CompareOp.LESS_THAN_OR_EQUAL;
        } 
        else if (depthTestState == DepthTestState.GEQUAL) {
            compareOp = CompareOp.GREATER_THAN_OR_EQUAL;
        }
        else if (depthTestState == DepthTestState.ALWAYS) {
            compareOp = CompareOp.ALWAYS_PASS;
        }
        return compareOp;
    }

    public static ColorTargetState toColorTargetState(BlendState blendState) {
        return blendState == BlendState.TRANSLUCENT ? B3DDefinitions.TRANSLUCENT_BLEND_COLOR_TARGET : B3DDefinitions.NONE;
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
        return format;
    }
}
