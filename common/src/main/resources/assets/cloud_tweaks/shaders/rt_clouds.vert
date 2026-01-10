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
    vec4 LightInformation; // x=LightCount, y=MaxShading, z=AmbientFactor (ambient value), w unused
};

int   LightCount         = int(min(LightInformation.x, float(MAX_LIGHT)));
float MaxShading         = LightInformation.y; // [0, 1]
float AmbientFactor      = LightInformation.z;
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

out float vertexDistance;
out vec4 vertexColor;

float fog_spherical_distance(vec3 pos) {
    return length(pos);
}

float lerp(float a, float b, float t) {
    return a + t * (b - a);
}

void main() {
    vec3 pos = Position + ModelOffset.xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fogEnabled() ? fog_spherical_distance(pos) : 0.0;

    float baseAlpha = usesCustomAlpha()
        ? BaseAlpha
        : Color.a;

    float finalAlpha = baseAlpha;

    if (FadeAlpha > 0.0) {
        float vertexY = Position.y;

        float normalizedY = clamp(vertexY / CloudBlockHeight, 0.0, 1.0);
        float dir = clamp(relY / TransitionRange, -1.0, 1.0);

        float fadeBelow = lerp(1.0, FadeAlpha, normalizedY);
        float fadeAbove = lerp(1.0, FadeAlpha, 1.0 - normalizedY);

        float mixVal = (dir + 1.0) * 0.5;
        float fade = lerp(fadeBelow, fadeAbove, mixVal);

        finalAlpha = baseAlpha * (1.0 - fade);
    }

    vec3 N = normalize(Normal);
    float lighting = 1;

    if (shadingEnabled()) {
        lighting = AmbientFactor;
        float shadeMask = float((Config & (1 << 1)) != 0);

        for (int i = 0; i < LightCount; i++) {
           vec3 L = normalize(LightDefinitions[i].xyz);
           float intensity = LightDefinitions[i].w;

           float ndl = max(dot(N, L), 0.0);
           lighting += ndl * intensity * shadeMask;
        }

        lighting = clamp(lighting, 0.0, MaxShading);
    }

    vec3 baseColor = Color.rgb * CloudColor.rgb;
    vec3 litColor = baseColor * lighting;

    vertexColor = vec4(litColor, fadeEnabled() ? finalAlpha : baseAlpha);
}
