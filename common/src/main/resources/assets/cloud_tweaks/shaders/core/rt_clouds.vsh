#version 330 core

in vec3 Position;
in vec4 Color;
in vec3 Normal;

#define MAX_LIGHT 32

uniform mat4 u_ProjMat;
uniform mat4 u_ModelViewMat;
uniform vec4 u_ModelOffset;

uniform vec4 u_CloudsInfo0; // Info0
uniform vec4 u_CloudsInfo1; // Info1
uniform vec4 u_CloudColor;
uniform vec4 u_FadeToColor;

uniform vec4 u_LightPos[MAX_LIGHT];
uniform vec4 u_LightColor[MAX_LIGHT];
uniform vec4 u_LightMeta;

uniform vec4 u_CameraPos;
uniform vec2 u_CloudHeight;

int   LightCount    = int(min(u_LightMeta.x, float(MAX_LIGHT)));
float MaxShading    = u_LightMeta.y;
float AmbientFactor = u_LightMeta.z;
bool  UsePhong      = (u_LightMeta.w > 0.5);

int   Config             = int(u_CloudsInfo0.x);
float BaseAlpha          = u_CloudsInfo0.w / 255.0;
float FadeAlpha          = u_CloudsInfo1.x / 255.0;
float TransitionRange    = u_CloudsInfo1.y;
float CloudBlockHeight   = u_CloudsInfo1.z;
float relY               = u_CloudsInfo1.w;

bool fogEnabled()        { return (Config & (1 << 0)) != 0; }
bool shadingEnabled()    { return (Config & (1 << 1)) != 0; }
bool usesCustomAlpha()   { return (Config & (1 << 2)) != 0; }
bool customBrightness()  { return (Config & (1 << 3)) != 0; }
bool usesCustomColor()   { return (Config & (1 << 4)) != 0; }
bool fadeEnabled()       { return (Config & (1 << 5)) != 0; }
bool colorFade()         { return (Config & (1 << 6)) != 0; }
bool invertedFade()      { return (Config & (1 << 7)) != 0; } // inverts both the fade color and alpha
bool useStaticFade()     { return (Config & (1 << 8)) != 0; } // static fade instead of dynamic positional fade

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
    vec3 pos = Position + u_ModelOffset.xyz;
    gl_Position = u_ProjMat * u_ModelViewMat * vec4(pos, 1.0);

    vWorldPos = Position + vec3(0, u_ModelOffset.y + u_CameraPos.y, 0);
    vDistance = fogEnabled() ? length(pos) : 0.0;

    float baseAlpha = usesCustomAlpha() ? BaseAlpha : Color.a;
    float finalAlpha = baseAlpha;

    if (FadeAlpha > 0.0) {
        if (useStaticFade()) {
            float staticRelY = u_CloudsInfo1.x;
            float ny = clamp(Position.y / CloudBlockHeight, 0.0, 1.0);
            float dir = clamp(staticRelY / TransitionRange, -1.0, 1.0);

            float fadeBelow = lerp(1.0, FadeAlpha, ny);
            float fadeAbove = lerp(1.0, FadeAlpha, 1.0 - ny);

            float fadeFactor = lerp(fadeBelow, fadeAbove, (dir + 1.0) * 0.5);

            if (invertedFade()) {
                fadeFactor = 1.0 - fadeFactor;
            }

            finalAlpha *= 1.0 - fadeFactor;
        } else {
            // Dynamic positional fade: based on camera position relative to layer
            float ny = clamp(Position.y / CloudBlockHeight, 0.0, 1.0);
            float dir = clamp(relY / TransitionRange, -1.0, 1.0);

            float fadeBelow = lerp(1.0, FadeAlpha, ny);
            float fadeAbove = lerp(1.0, FadeAlpha, 1.0 - ny);

            float fadeFactor = lerp(fadeBelow, fadeAbove, (dir + 1.0) * 0.5);

            if (invertedFade()) {
                fadeFactor = 1.0 - fadeFactor;
            }

            finalAlpha *= 1.0 - fadeFactor;
        }
    }

    vec3 baseColor = Color.rgb * u_CloudColor.rgb;
    vec3 N = normalize(Normal);

    float lighting = 1.0;

    if (shadingEnabled() && !UsePhong) {
        lighting = AmbientFactor;

        for (int i = 0; i < LightCount; i++) {
            vec3 lightDir = u_LightPos[i].xyz;
            vec3 L = normalize(-lightDir);
            lighting += max(dot(N, L), 0.0) * u_LightColor[i].w;
        }

        lighting = clamp(lighting, 0.0, MaxShading);
    }

    // bottom being the actual base color, top being the mixed color
    if (fadeEnabled() && colorFade()) {
        float ratio = (baseAlpha > 0.0) ? (finalAlpha / baseAlpha) : 0.0;
        float colorFadeFactor = 1.0 - ratio;

        if (invertedFade()) {
            colorFadeFactor = 1.0 - colorFadeFactor;
        }

        baseColor = mix(baseColor, u_FadeToColor.rgb, colorFadeFactor);
    }

    vNormal = N;
    vColor  = vec4(baseColor * lighting, fadeEnabled() ? finalAlpha : baseAlpha);
}