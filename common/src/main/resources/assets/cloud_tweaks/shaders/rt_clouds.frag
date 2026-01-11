#version 330 core

#define MAX_LIGHT 32

layout(std140) uniform CloudInfo {
    ivec4 Info0;
    vec4  Info1;
    vec4  CloudColor;
};

layout(std140) uniform Lighting {
    vec4 LightDefinitions[MAX_LIGHT]; // xyz = light POSITION, w = intensity
    vec4 LightColors[MAX_LIGHT];
    vec4 LightInformation; // x=count, y=maxShading, z=ambient, w=shadingMode
};

int   LightCount     = int(min(LightInformation.x, float(MAX_LIGHT)));
float MaxShading     = LightInformation.y;
float AmbientFactor  = LightInformation.z;
bool  UsePhong       = (LightInformation.w > 0.5);

int Config   = Info0.x;
int FogStart = Info0.y;
int FogEnd   = Info0.z;

bool fogEnabled()     { return (Config & (1 << 0)) != 0; }
bool shadingEnabled() { return (Config & (1 << 1)) != 0; }

in float vDistance;
in vec4  vColor;
in vec3  vNormal;
in vec3  vWorldPos;

out vec4 fragColor;

float linearFog(float d, float a, float b) {
    return clamp((d - a) / (b - a), 0.0, 1.0);
}

void main() {
    vec4 color = vColor;

    if (UsePhong && shadingEnabled()) {
        vec3 N = normalize(vNormal);
        float lighting = AmbientFactor;

        for (int i = 0; i < LightCount; i++) {
            vec3 lightPos = LightDefinitions[i].xyz;
            vec3 L = normalize(lightPos - vWorldPos);

            lighting += max(dot(N, L), 0.0) * LightDefinitions[i].w;
        }

        lighting = clamp(lighting, 0.0, MaxShading);
        color.rgb *= lighting;
    }

    if (fogEnabled()) {
        color.a *= 1.0 - linearFog(vDistance, FogStart, FogEnd);
    }

    fragColor = color;
}
