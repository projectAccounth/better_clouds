#version 330 core

#define MAX_LIGHT 32

uniform mat4 u_ProjMat;
uniform mat4 u_ModelViewMat;
uniform vec4 u_ModelOffset;

uniform vec4 u_CloudsInfo0; // Info0
uniform vec4 u_CloudsInfo1; // Info1
uniform vec4 u_CloudColor;
uniform vec4 u_FadeToColor;
uniform vec4 u_FadeInfo;

uniform vec4 u_LightPos[MAX_LIGHT];
uniform vec4 u_LightColor[MAX_LIGHT];
uniform vec4 u_LightMeta;

uniform vec4 u_CameraPos;
uniform vec2 u_CloudHeight;

int   LightCount    = int(min(u_LightMeta.x, float(MAX_LIGHT)));
float MaxShading    = u_LightMeta.y;
float AmbientFactor = u_LightMeta.z;
bool  UsePhong      = (u_LightMeta.w > 0.5);

int Config   = int(u_CloudsInfo0.x);
int FogStart = int(u_CloudsInfo0.y);
int FogEnd   = int(u_CloudsInfo0.z);

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
        vec3 N = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));
        vec3 V = normalize(u_CameraPos.xyz - vWorldPos);

        float lighting = AmbientFactor;
        vec3  silverAccum = vec3(0.0);

        for (int i = 0; i < LightCount; i++) {
            vec3 lightDir = u_LightPos[i].xyz;
            float intensity = u_LightColor[i].w;

            vec3 L = normalize(-lightDir);

            float ndl = max(dot(N, L), 0.0);
            lighting += ndl * intensity;
            
            float backLight = max(dot(-L, V), 0.0);
            float edge = 1.0 - ndl;

            float viewGrazing = pow(1.0 - max(dot(N, V), 0.0), 2.0);

            float silver = backLight * edge * viewGrazing;
            vec3 silverColor = vec3(1.0, 0.95, 0.9);

            silverAccum += silver * silverColor * intensity * 0.75;
        }

        lighting = clamp(lighting, 0.0, MaxShading);

        color.rgb = color.rgb * lighting + silverAccum;
    }

    if (fogEnabled()) {
        color.a *= 1.0 - linearFog(vDistance, FogStart, FogEnd);
    }

    fragColor = color;
}
