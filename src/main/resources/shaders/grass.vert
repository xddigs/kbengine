#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;

out vec2 vTexCoord;
out vec3 vNormal;
out vec4 vLightSpacePosition;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform mat4 uLightSpaceMatrix;

void main() {
    vec4 worldPosition = uModel * vec4(aPos, 1.0);
    gl_Position = uProjection * uView * worldPosition;
    vTexCoord = aTexCoord;
    vNormal = normalize(mat3(transpose(inverse(uModel))) * aNormal);
    vLightSpacePosition = uLightSpaceMatrix * worldPosition;
}
