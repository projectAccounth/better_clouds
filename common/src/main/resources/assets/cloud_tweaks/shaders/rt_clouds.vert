#version 150

in vec3 Position;
in vec4 Color;

uniform vec4 CloudColor;
uniform int Config; 
uniform int CloudFogStart;
uniform int CloudFogEnd;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ModelOffset;
uniform int FogShape;
uniform vec4 ColorModulator;

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
    vertexColor = Color * ColorModulator * vec4(CloudColor.rgb, 1.0f);
}
