#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:fog.glsl>

layout(std140) uniform Model {
    vec4 unused;
    int config; 
    int pad0;
    int pad1;
    int pad2;
};

in vec3 Position;
in vec4 Color;

bool fogEnabled() { return (config & (1 << 0)) != 0; }

out float vertexDistance;
out vec4 vertexColor;

void main() {
    vec3 pos = Position + ModelOffset;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_spherical_distance(pos);
    vertexColor = Color * ColorModulator;
}
