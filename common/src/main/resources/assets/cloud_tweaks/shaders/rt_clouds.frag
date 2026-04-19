#version 330 core

#define MAX_LIGHT 32

layout(std140) uniform Transforms {
    mat4 ProjMat;
    mat4 ModelViewMat;
    vec4 MOffset;
};

layout(std140) uniform CloudInfo {
    ivec4 Info0;
    vec4  Info1;
    vec4  CloudColor;
    vec4  FadeToColor;
    vec4  FadeInfo; // x=StaticFadeRelY
};

layout(std140) uniform Lighting {
    vec4 LightDefinitions[MAX_LIGHT]; // xyz = POSITION, w = intensity
    vec4 LightColors[MAX_LIGHT];
    vec4 LightInformation; // x=count y=max z=ambient w=usePhong
};

layout(std140) uniform Camera {
    vec4 CameraPosition;
};

int   LightCount    = int(min(LightInformation.x, float(MAX_LIGHT)));
float MaxShading    = LightInformation.y;
float AmbientFactor = LightInformation.z;
bool  UsePhong      = (LightInformation.w > 0.5);

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
        vec3 N = normalize(cross(dFdx(vWorldPos), dFdy(vWorldPos)));
        vec3 V = normalize(CameraPosition.xyz - vWorldPos);

        float lighting = AmbientFactor;
        vec3  silverAccum = vec3(0.0);

        for (int i = 0; i < LightCount; i++) {
            vec3 lightPos = LightDefinitions[i].xyz;
            float intensity = LightDefinitions[i].w;

            vec3 L = normalize(lightPos);

            float ndl = max(dot(N, -L), 0.0);
            lighting += ndl * intensity;
            
            float backLight = max(dot(L, V), 0.0);
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
