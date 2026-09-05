#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;

out vec2 vTexCoord;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;
uniform vec4 uUVBounds;
uniform vec3 uShapeMin;
uniform vec3 uShapeMax;

void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
    vec3 localPosition = mix(uShapeMin, uShapeMax, aPos);
    vec2 shapeUV;
    if (abs(aNormal.y) > 0.5) {
        shapeUV = aNormal.y > 0.0
                ? vec2(localPosition.x, localPosition.z)
                : vec2(localPosition.x, 1.0 - localPosition.z);
    } else if (aNormal.x > 0.5) {
        shapeUV = vec2(1.0 - localPosition.z, localPosition.y);
    } else if (aNormal.x < -0.5) {
        shapeUV = vec2(localPosition.z, localPosition.y);
    } else if (aNormal.z < -0.5) {
        shapeUV = vec2(1.0 - localPosition.x, localPosition.y);
    } else {
        shapeUV = vec2(localPosition.x, localPosition.y);
    }
    vTexCoord = mix(uUVBounds.xy, uUVBounds.zw, shapeUV);
}
