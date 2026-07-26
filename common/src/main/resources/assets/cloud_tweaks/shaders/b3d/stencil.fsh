#version 330 core

in vec2 TexCoords;

uniform sampler2D colorTex0;
uniform sampler2D depthTex0;

uniform sampler2D colorTex1;
uniform sampler2D depthTex1;

out vec4 FragColor;

void main() {
    vec4 colorBase = texture(colorTex0, TexCoords);
    vec4 colorOverride = texture(colorTex1, TexCoords);
    
    float depthBase = texture(depthTex0, TexCoords).r;
    float depthOverride = texture(depthTex1, TexCoords).r;

    bool hasGeometry = colorOverride.a > 0.0;

    if (hasGeometry) {
        FragColor = colorOverride;
        gl_FragDepth = depthOverride;
    } else {
        FragColor = colorBase;
        gl_FragDepth = depthBase;
    }
}