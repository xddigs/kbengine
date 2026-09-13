#version 330 core
layout(location = 0) in vec3 aPosition;
uniform vec3 uOrigin;
uniform mat4 uLightSpaceMatrix;
void main() {
    gl_Position = uLightSpaceMatrix * vec4(aPosition * 0.25 + uOrigin, 1.0);
}
