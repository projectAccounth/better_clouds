#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(std140) uniform Transforms {
    mat4 ProjMat;
    mat4 ModelViewMat;
    vec4 ModelOffset;
};

layout(std140) uniform CloudInfo {
    ivec4 Info0;   // x=Config, y=FogStart, z=FogEnd, w=BaseAlpha
    vec4  Info1;   // x=FadeAlpha, y=TransitionRange, z=CloudBlockHeight, w=unused
    vec4  CloudColor;
};

int Config = Info0.x;
int CloudFogStart = Info0.y;
int CloudFogEnd = Info0.z;
int BaseAlpha = Info0.w;
int FadeAlpha = int(Info1.x);
float TransitionRange = Info1.y;
float CloudBlockHeight = Info1.z;

bool fogEnabled() { return (Config & (1 << 0)) != 0; }
bool shadingEnabled() { return (Config & (1 << 1)) != 0; }
bool usesCustomAlpha() { return (Config & (1 << 2)) != 0; }
bool customBrightness() { return (Config & (1 << 3)) != 0; }
bool usesCustomColor() { return (Config & (1 << 4)) != 0; }

out float vertexDistance;
out vec4 vertexColor;

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

void main() {
    vec3 pos = Position + ModelOffset.xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fogEnabled() ? fog_spherical_distance(pos) : 0.0;
    vertexColor = Color * vec4(CloudColor.rgb, 1.0f);
}
