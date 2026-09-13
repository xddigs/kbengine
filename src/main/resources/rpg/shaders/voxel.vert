#version 330 core
layout(location=0) in vec3 aCell;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec3 aColor;
uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uOrigin;
out vec3 vNormal;
out vec3 vColor;
out vec3 vWorld;
void main() {
    vWorld = uOrigin + aCell * 0.25;
    vNormal = aNormal;
    vColor = aColor;
    gl_Position = uProjection * uView * vec4(vWorld, 1.0);
}
