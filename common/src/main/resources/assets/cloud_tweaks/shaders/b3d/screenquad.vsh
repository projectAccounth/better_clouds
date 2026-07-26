#version 330

out vec2 TexCoords;

void main() {
    uint id = uint(gl_VertexID);
    TexCoords = vec2((id << 1) & 2u, id & 2u);
    
    gl_Position = vec4(TexCoords * 2.0 - 1.0, 0.0, 1.0);
}
