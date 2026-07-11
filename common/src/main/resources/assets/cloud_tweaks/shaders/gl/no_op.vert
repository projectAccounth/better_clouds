#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec3 Normal;

layout(std140) uniform Transforms {
    vec4 MOffset;
    mat4 ProjMat;
    mat4 ModelViewMat;
};

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + MOffset.xyz, 1.0);
}