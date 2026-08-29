package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.not_thefirst.lib.gl_render_system.alt.SamplerDefinition.WrapMode;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
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
        else if (depthTestState == DepthTestState.LESS) {
            compareOp = CompareOp.LESS_THAN;
        }
        else if (depthTestState == DepthTestState.GEQUAL) {
            compareOp = CompareOp.GREATER_THAN_OR_EQUAL;
        }
        else if (depthTestState == DepthTestState.GREATER) {
            compareOp = CompareOp.GREATER_THAN;
        }
        else if (depthTestState == DepthTestState.ALWAYS) {
            compareOp = CompareOp.ALWAYS_PASS;
        }
        else if (depthTestState == DepthTestState.NEVER) {
            compareOp = CompareOp.NEVER_PASS;
        }
        return compareOp;
    }

    public static ColorTargetState toColorTargetState(BlendState blendState) {
        if (blendState == BlendState.TRANSLUCENT)
            return B3DDefinitions.TRANSLUCENT_BLEND_COLOR_TARGET;
        else if (blendState == BlendState.OPAQUE)
            return B3DDefinitions.OPAQUE;
        else if (blendState == BlendState.PREMULTIPLIED_ALPHA)
            return B3DDefinitions.PREMULTIPLIED_COLOR_TARGET;
        else if (blendState == BlendState.ADDITIVE)
            return B3DDefinitions.ADDITIVE_COLOR_TARGET;
        else if (blendState == BlendState.ADD)
            return B3DDefinitions.ADD_COLOR_TARGET;
        else if (blendState == BlendState.SCREEN)
            return B3DDefinitions.SCREEN_COLOR_TARGET;
        return null;
    }

    public static FilterMode toFilter(net.not_thefirst.lib.gl_render_system.alt.SamplerDefinition.FilterMode def) {
        switch (def) {
            case NEAREST: return FilterMode.NEAREST;
            case LINEAR: return FilterMode.LINEAR;
            default: throw new IllegalArgumentException("Unknown filter mode " + def);
        }
    }

    public static AddressMode toAddr(WrapMode def) {
        switch (def) {
            case CLAMP_TO_EDGE: return AddressMode.CLAMP_TO_EDGE;
            case REPEAT: return AddressMode.REPEAT;
            default: throw new IllegalArgumentException("Unknown wrap mode " + def);
        }
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

    public static PrimitiveTopology toPrimitiveTopology(GLPrimitive primitive) {
        switch (primitive) {
            case TRIANGLES: return PrimitiveTopology.TRIANGLES;
            case LINES: return PrimitiveTopology.LINES;
            case QUADS: return PrimitiveTopology.QUADS;
            default: return null;
        }
    }
}
