#version 300
#moj_import <minecraft:fog.glsl>

in float vVertexDistance;
in vec4  vColor;

out vec4 fragColor;

layout(std140) uniform CloudInfo {
    vec4 CloudColor;
    vec4 CloudOffset;
    vec4 CellSize;
    float baseAlpha;
    float fadeAlpha;
    float brightness;
    float transitionRange;
    float cloudLayerSpacing;
    int layerIdx;
    int configFlags;
    float relYToCenter;
};

bool fogEnabled() { return (configFlags & (1 << 5)) != 0; }

void main() {
    vec4 color = vColor;
    if (fogEnabled()) {
        float fogMask = 1.0 - linear_fog_value(vVertexDistance, 0.0, FogCloudsEnd);
        color.a *= fogMask;
    }
    fragColor = color;
}
