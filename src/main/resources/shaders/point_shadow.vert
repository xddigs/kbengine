#version 330 core
layout(location = 0) in vec3 aPos;
uniform mat4 uModel;
uniform mat4 uLightSpaceMatrix;
out vec3 vWorldPosition;
void main() {
    vec4 worldPosition = uModel * vec4(aPos, 1.0);
    vWorldPosition = worldPosition.xyz;
    gl_Position = uLightSpaceMatrix * worldPosition;
}
