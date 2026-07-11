#version 330 core

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec3 Normal;

layout(std140) uniform Transforms {
    vec4 MOffset;
    mat4 M0;
    mat4 M1;
};

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + MOffset.xyz, 1.0);
}
