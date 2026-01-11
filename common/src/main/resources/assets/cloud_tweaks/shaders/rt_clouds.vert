#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec3 Normal;

#define MAX_LIGHT 32

layout(std140) uniform Transforms {
    mat4 ProjMat;
    mat4 ModelViewMat;
    vec4 ModelOffset;
};

layout(std140) uniform CloudInfo {
    ivec4 Info0;   // x=Config, y=FogStart, z=FogEnd, w=BaseAlpha
    vec4  Info1;   // x=FadeAlpha, y=TransitionRange, z=CloudBlockHeight, w=relY
    vec4  CloudColor;
};

layout(std140) uniform Lighting {
    vec4 LightDefinitions[MAX_LIGHT]; // xyz = direction, w = intensity
    vec4 LightColors[MAX_LIGHT]; // rgb, alpha unused
    vec4 LightInformation; // x=LightCount, y=MaxShading, z=Ambient, w=ShadingMode
};

int   LightCount         = int(min(LightInformation.x, float(MAX_LIGHT)));
float MaxShading         = LightInformation.y;
float AmbientFactor      = LightInformation.z;
bool  UsePhong           = (LightInformation.w > 0.5);

int   Config             = Info0.x;
float BaseAlpha          = float(Info0.w) / 255.0f;
float FadeAlpha          = Info1.x / 255.0f;
float TransitionRange    = Info1.y;
float CloudBlockHeight   = Info1.z;
float relY               = Info1.w;

bool fogEnabled()        { return (Config & (1 << 0)) != 0; }
bool shadingEnabled()    { return (Config & (1 << 1)) != 0; }
bool usesCustomAlpha()   { return (Config & (1 << 2)) != 0; }
bool customBrightness()  { return (Config & (1 << 3)) != 0; }
bool usesCustomColor()   { return (Config & (1 << 4)) != 0; }
bool fadeEnabled()       { return (Config & (1 << 5)) != 0; }

out float vDistance;
out vec4  vColor;
out vec3  vNormal;
out vec3  vWorldPos;

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float lerp(float a, float b, float t) {
    return a + t * (b - a);
}

void main() {
    vec3 pos = Position + ModelOffset.xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vWorldPos = pos;
    vDistance = fogEnabled() ? length(pos) : 0.0;

    float baseAlpha = usesCustomAlpha() ? BaseAlpha : Color.a;
    float finalAlpha = baseAlpha;

    if (FadeAlpha > 0.0) {
        float ny = clamp(Position.y / CloudBlockHeight, 0.0, 1.0);
        float dir = clamp(relY / TransitionRange, -1.0, 1.0);

        float fadeBelow = lerp(1.0, FadeAlpha, ny);
        float fadeAbove = lerp(1.0, FadeAlpha, 1.0 - ny);
        finalAlpha *= lerp(fadeBelow, fadeAbove, (dir + 1.0) * 0.5);
    }

    vec3 baseColor = Color.rgb * CloudColor.rgb;
    vec3 N = normalize(Normal);

    float lighting = 1.0;

    if (shadingEnabled() && !UsePhong) {
        lighting = AmbientFactor;

        for (int i = 0; i < LightCount; i++) {
            vec3 lightPos = LightDefinitions[i].xyz;
            vec3 L = normalize(lightPos - pos);
            lighting += max(dot(N, L), 0.0) * LightDefinitions[i].w;
        }

        lighting = clamp(lighting, 0.0, MaxShading);
    }

    vNormal = N;
    vColor  = vec4(baseColor * lighting, fadeEnabled() ? finalAlpha : baseAlpha);
}