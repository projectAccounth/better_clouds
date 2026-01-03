#version 150

#moj_import <minecraft:fog.glsl>

layout(std140) uniform Model {
    vec4 CloudColor;
    int config; 
    int cloudFogStart;
    int cloudFogEnd;
    int pad2;
};

bool fogEnabled() { return (config & (1 << 0)) != 0; }

in float vertexDistance;
in vec4 vertexColor;

out vec4 fragColor;

float linearFog(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 0.0;
    } else if (vertexDistance >= fogEnd) {
        return 1.0;
    }

    return (vertexDistance - fogStart) / (fogEnd - fogStart);
}

void main() {
    vec4 color = vertexColor;
    color.a *= 1.0 - (fogEnabled() ? linearFog(vertexDistance, cloudFogStart, cloudFogEnd) : 0.0);
    fragColor = color;
}
