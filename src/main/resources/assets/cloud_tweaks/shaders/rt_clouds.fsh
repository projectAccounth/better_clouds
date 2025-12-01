#version 150

#moj_import <minecraft:fog.glsl>

layout(std140) uniform Model {
    vec4 unused;
    int config; 
    int pad0;
    int pad1;
    int pad2;
};

bool fogEnabled() { return (config & (1 << 0)) != 0; }

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;
    if (fogEnabled()) {
        float fogMask = 1.0 - linear_fog_value(vertexDistance, 0.0, FogCloudsEnd);
        color.a *= fogMask;
    }
    fragColor = color;
}
