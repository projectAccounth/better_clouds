#version 330 core

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec3 Normal;

layout(std140) uniform Transforms {
    vec4 MOffset;
    mat4 M0;
    mat4 M1;
};

layout(std140) uniform CloudInfo {
    ivec4 Info0;   // x=Config, y=FogStart, z=FogEnd, w=BaseAlpha
    vec4  Info1;   // x=FadeAlpha, y=TransitionRange, z=CloudBlockHeight, w=relY
    vec4  CloudColor;
    vec4  FadeToColor;
    vec4  Info2; // x=StaticFadeRelY
    vec4  SkyColor;
};

layout(std140) uniform Camera {
    vec4 CameraPosition;
};

layout(std140) uniform Outline {
    vec4 OutlineColor; // RGBA
    vec4 OutlineInfo1; // x=outlineConfig, y=outlineBrightness, z=outlineAlpha, w=unused
};

int   Config             = Info0.x;
float BaseAlpha          = float(Info0.w) / 255.0f;
float FadeAlpha          = Info1.x / 255.0f;
float TransitionRange    = Info1.y;
float CloudBlockHeight   = Info1.z;
float relY               = Info1.w;

bool fogEnabled()        { return (Config & (1 << 0)) != 0; }
bool usesCustomAlpha()   { return (Config & (1 << 2)) != 0; }
bool fadeEnabled()       { return (Config & (1 << 5)) != 0; }
bool colorFade()         { return (Config & (1 << 6)) != 0; }
bool invertedFade()      { return (Config & (1 << 7)) != 0; }
bool useStaticFade()     { return (Config & (1 << 8)) != 0; }

bool outlineCustomBrightness() { return (int(OutlineInfo1.x) & (1 << 0)) != 0; }
bool outlineOverrideTextureColor() { return (int(OutlineInfo1.x) & (1 << 1)) != 0; }

vec3 computeOutlineColor(vec3 baseColor) {
    vec3 skyFactor = outlineCustomBrightness() ? vec3(1.0) : SkyColor.rgb;
    return baseColor * OutlineColor.rgb * skyFactor * OutlineInfo1.y;
}

float computeOutlineAlpha(float alpha) {
    return alpha * OutlineInfo1.z;
}

out float vDistance;
out vec4  vColor;
out vec3  vWorldPos;

float lerp(float a, float b, float t) {
    return a + t * (b - a);
}

void main() {
    vec3 pos = Position + MOffset.xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vWorldPos = Position + vec3(0, MOffset.y + CameraPosition.y, 0);
    vDistance = fogEnabled() ? length(pos) : 0.0;

    float baseAlpha = usesCustomAlpha() ? BaseAlpha : Color.a;
    float finalAlpha = baseAlpha;

    // Apply fade effect
    if (FadeAlpha > 0.0) {
        if (useStaticFade()) {
            float staticRelY = Info2.x;
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

    // Determine base color (outlines use vertex color or white)
    vec3 baseColor = outlineOverrideTextureColor() ? Color.rgb : vec3(1.0);

    // Compute outline color and alpha
    vec3 outlineColor = computeOutlineColor(baseColor);
    float outlineAlpha = computeOutlineAlpha(fadeEnabled() ? finalAlpha : baseAlpha);

    vColor = vec4(outlineColor, outlineAlpha);
}
