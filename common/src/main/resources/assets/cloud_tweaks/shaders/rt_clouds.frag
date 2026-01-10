#version 330 core

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
    color.a *= 1.0 - int(fogEnabled()) * linearFog(vertexDistance, CloudFogStart, CloudFogEnd);
    fragColor = color;
}
