#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:fog.glsl>

layout(std140) uniform Model {
    vec4 CloudColor;
    int config; 
    int cloudFogStart;
    int cloudFogEnd;
    int pad2;
};

in vec3 Position;
in vec4 Color;

bool fogEnabled() { return (config & (1 << 0)) != 0; }
bool shadingEnabled() { return (config & (1 << 1)) != 0; }
bool usesCustomAlpha() { return (config & (1 << 2)) != 0; }
bool customBrightness() { return (config & (1 << 3)) != 0; }
bool usesCustomColor() { return (config & (1 << 4)) != 0; }

out float vertexDistance;
out vec4 vertexColor;

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fogEnabled() ? fog_spherical_distance(pos) : 0.0;
    vertexColor = Color * ColorModulator * vec4(CloudColor.rgb, 1.0f);
}
