#version 150

#moj_import <minecraft:fog.glsl>

uniform vec4 CloudColor;
uniform int Config; 
uniform int CloudFogStart;
uniform int CloudFogEnd;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ModelOffset;
uniform int FogShape;
uniform vec4 ColorModulator;

bool fogEnabled() { return (Config & (1 << 0)) != 0; }

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
    color.a *= 1.0 - (fogEnabled() ? linearFog(vertexDistance, CloudFogStart, CloudFogEnd) : 0.0);
    fragColor = color;
}
