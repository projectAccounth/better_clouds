#version 330 core
out vec4 FragColor;
in vec2 TexCoords;

uniform sampler2D colorTex0;
uniform sampler2D depthTex0;

uniform sampler2D colorTex1;
uniform sampler2D depthTex1;

void main() {
    vec4 colorA = texture(colorTex0, TexCoords);
    vec4 colorB = texture(colorTex1, TexCoords);
    
    float depthA = texture(depthTex0, TexCoords).r;
    float depthB = texture(depthTex1, TexCoords).r;

    if (depthA > depthB) {
        FragColor = colorA;
        gl_FragDepth = depthA; 
    } else {
        FragColor = colorB;
        gl_FragDepth = depthB;
    }
}