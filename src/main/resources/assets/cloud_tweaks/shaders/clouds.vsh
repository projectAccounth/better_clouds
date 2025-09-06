#version 150
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

const int FLAG_MASK_DIR      = 7;
const int FLAG_INSIDE_FACE   = 1 << 4;
const int FLAG_USE_TOP_COLOR = 1 << 5;
const int FLAG_EXTRA_Z       = 1 << 6;
const int FLAG_EXTRA_X       = 1 << 7;

layout(std140) uniform CloudInfo {
    vec4 CloudColor;
    vec4 CloudOffset;
    vec4 CellSize;          
    float baseAlpha;
    float fadeAlpha;
    float brightness;
    float transitionRange;
    float cloudLayerSpacing;
    int layerIdx;
    int configFlags;
    float relYToCenter;      
};

bool isEnabled()        { return (configFlags & (1 << 0)) != 0; }
bool appearsShaded()    { return (configFlags & (1 << 1)) != 0; }
bool customBrightness() { return (configFlags & (1 << 3)) != 0; }
bool usesCustomAlpha()  { return (configFlags & (1 << 2)) != 0; }
bool fadeEnabled()      { return (configFlags & (1 << 6)) != 0; }
bool fogEnabled()       { return (configFlags & (1 << 5)) != 0; }

uniform isamplerBuffer CloudFaces;

out float vVertexDistance;
out vec4  vColor; // final pre-fog color (rgb+alpha)

const vec3[] vertices = vec3[](
    vec3(1,0,0),vec3(1,0,1),vec3(0,0,1),vec3(0,0,0),
    vec3(0,1,0),vec3(0,1,1),vec3(1,1,1),vec3(1,1,0),
    vec3(0,0,0),vec3(0,1,0),vec3(1,1,0),vec3(1,0,0),
    vec3(1,0,1),vec3(1,1,1),vec3(0,1,1),vec3(0,0,1),
    vec3(0,0,1),vec3(0,1,1),vec3(0,1,0),vec3(0,0,0),
    vec3(1,0,0),vec3(1,1,0),vec3(1,1,1),vec3(1,0,1)
);

const vec4 faceColors[6] = vec4[](
    vec4(0.7,0.7,0.7,0.8),
    vec4(1.0,1.0,1.0,0.8),
    vec4(0.8,0.8,0.8,0.8),
    vec4(0.8,0.8,0.8,0.8),
    vec4(0.9,0.9,0.9,0.8),
    vec4(0.9,0.9,0.9,0.8)
);

// CPU lerp: lerp(d,e,f) == e + d * (f - e)
float cpu_lerp(float d, float e, float f) {
    return e + d * (f - e);
}

void main() {
    int quadVertex = gl_VertexID % 4;
    int index = (gl_VertexID / 4) * 3;

    int cellX = texelFetch(CloudFaces, index).r;
    int cellZ = texelFetch(CloudFaces, index + 1).r;
    int dirAndFlags = texelFetch(CloudFaces, index + 2).r;

    int direction = dirAndFlags & FLAG_MASK_DIR;
    bool isInsideFace = (dirAndFlags & FLAG_INSIDE_FACE) != 0;
    bool useTopColor  = (dirAndFlags & FLAG_USE_TOP_COLOR) != 0;

    cellX = (cellX << 1) | ((dirAndFlags & FLAG_EXTRA_X) >> 7);
    cellZ = (cellZ << 1) | ((dirAndFlags & FLAG_EXTRA_Z) >> 6);

    vec3 faceVertex = vertices[(direction * 4) + (isInsideFace ? (3 - quadVertex) : quadVertex)];

    // world position
    vec3 pos = (faceVertex * CellSize.xyz) + (vec3(cellX, 0, cellZ) * CellSize.xyz) + CloudOffset.xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vVertexDistance = fogEnabled() ? fog_spherical_distance(pos) : 0.0;

    // === Pre-fade RGB (CPU handles tinting; shader only applies brightness & shading if requested) ===
    vec3 rgb = CloudColor.rgb;

    if (customBrightness()) 
        rgb *= brightness;
    if (appearsShaded())
        rgb *= (useTopColor ? faceColors[1].rgb : faceColors[direction].rgb);

    // alpha base
    float baseA = usesCustomAlpha() ? baseAlpha : CloudColor.a;
    float finalA = baseA;

    if (fadeEnabled()) {
        float cloudHeight = CellSize.y;

        // vertexY as local chunk height (0 or cloudHeight) — matches CPU's vertexY input to recolor(...)
        float vertexY = faceVertex.y * cloudHeight;

        // normalizedY in [0..1]
        float normalizedY = clamp(vertexY / cloudHeight, 0.0, 1.0);

        // dir uses CPU relY / transitionRange
        float dir = 0.0;
        if (transitionRange != 0.0) {
            dir = clamp(relYToCenter / transitionRange, -1.0, 1.0);
            // If behaviour inverted, try the opposite sign below:
            // dir = clamp(-relYToCenter / transitionRange, -1.0, 1.0);
        }

        float fadeBelow = cpu_lerp(normalizedY, 1.0, fadeAlpha);
        float fadeAbove = cpu_lerp(1.0 - normalizedY, 1.0, fadeAlpha);

        float mixFactor = (dir + 1.0) * 0.5;
        float fade = mix(fadeBelow, fadeAbove, mixFactor);

        finalA = baseA * (1.0 - fade);
    }

    vColor = vec4(rgb, finalA);

    if (!isEnabled()) {
        vColor = CloudColor * (useTopColor ? faceColors[1] : faceColors[direction]);
    }
}
