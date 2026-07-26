#version 330 core

layout(std140) uniform CloudInfo {
    ivec4 Info0;
    vec4  Info1;
    vec4  CloudColor;
    vec4  FadeToColor;
    vec4  Info2;
    vec4  SkyColor;
};

layout(std140) uniform Camera {
    vec4 CameraPosition;
};

layout(std140) uniform Outline {
    vec4 OutlineColor;
    vec4 OutlineInfo1;
};

int Config   = Info0.x;
int FogStart = Info0.y;
int FogEnd   = Info0.z;

bool fogEnabled()     { return (Config & (1 << 0)) != 0; }

in float vDistance;
in vec4  vColor;
in vec3  vWorldPos;

out vec4 fragColor;

float linearFog(float d, float a, float b) {
    return clamp((d - a) / (b - a), 0.0, 1.0);
}

void main() {
    vec4 color = vColor;

    if (fogEnabled()) {
        color.a *= 1.0 - linearFog(vDistance, FogStart, FogEnd);
    }

    fragColor = color;
}
