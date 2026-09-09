#version 330 core

layout(location = 0) in vec3 aPos;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform vec2 uViewportSize;
uniform vec2 uOutlineOffset;

void main() {
    vec4 clipPosition = uProjection * uView * uModel * vec4(aPos, 1.0);
    vec2 pixelOffset = (uOutlineOffset * 2.0) / max(uViewportSize, vec2(1.0));
    clipPosition.xy += pixelOffset * clipPosition.w;
    gl_Position = clipPosition;
}
