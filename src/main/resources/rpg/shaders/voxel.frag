#version 330 core
in vec3 vNormal;
in vec3 vColor;
in vec3 vWorld;
uniform vec3 uLightDirection;
uniform vec3 uSunColor;
uniform float uLightIntensity;
uniform float uAmbientIntensity;
uniform float uAlpha;
uniform bool uSelected;
uniform vec3 uSelectedMin;
uniform bool uEnableShadows;
uniform sampler2D uShadowMap;
uniform mat4 uLightSpaceMatrix;
uniform int uViewMode;
uniform vec3 uViewPlayerPosition;
uniform vec3 uViewCameraPosition;
uniform vec4 uViewBounds;
uniform float uViewRadius;
uniform float uViewFloorY;
uniform float uViewCeilingY;
uniform float uViewFogStrength;
uniform bool uIgnoreViewFog;
out vec4 FragColor;

float shadowAmount() {
    if (!uEnableShadows) return 0.0;
    vec4 light = uLightSpaceMatrix * vec4(vWorld, 1.0);
    vec3 p = light.xyz / light.w * 0.5 + 0.5;
    if (any(lessThan(p, vec3(0.0))) || any(greaterThan(p, vec3(1.0)))) return 0.0;
    float bias = max(0.0002, 0.001 * (1.0 - dot(vNormal, -normalize(uLightDirection))));
    vec2 texel = 1.0 / vec2(textureSize(uShadowMap, 0));
    float result = 0.0;
    for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++)
        result += p.z - bias > texture(uShadowMap, p.xy + vec2(x, y) * texel).r ? 1.0 : 0.0;
    return result / 9.0;
}

float applyViewFog(vec3 p) {
    if (uIgnoreViewFog || uViewMode == 0 || uViewFogStrength <= 0.0) return 1.0;
    vec2 offset = p.xz - uViewPlayerPosition.xz;
    vec2 camera = uViewCameraPosition.xz - uViewPlayerPosition.xz;
    vec2 front = length(camera) > 0.001 ? normalize(camera) : vec2(0.7071);
    bool outside = p.y >= uViewCeilingY;
    float visibility = 1.0;
    if (uViewMode == 1) {
        outside = outside || p.x < uViewBounds.x || p.z < uViewBounds.y
                || p.x > uViewBounds.z || p.z > uViewBounds.w;
        bool wall = (front.x > 0.15 && p.x > uViewBounds.z - 0.26)
                || (front.x < -0.15 && p.x < uViewBounds.x + 0.26)
                || (front.y > 0.15 && p.z > uViewBounds.w - 0.26)
                || (front.y < -0.15 && p.z < uViewBounds.y + 0.26);
        outside = outside || (wall && p.y > uViewFloorY + 0.08);
        float edge = min(min(p.x - uViewBounds.x, uViewBounds.z - p.x),
                         min(p.z - uViewBounds.y, uViewBounds.w - p.z));
        visibility = mix(0.35, 1.0, smoothstep(0.0, 1.25, edge));
    } else {
        float distance = length(offset);
        outside = outside || distance > uViewRadius;
        visibility = 1.0 - smoothstep(max(0.0, uViewRadius - 2.0), uViewRadius, distance);
        if (dot(offset, front) > 0.20 && abs(dot(offset, vec2(-front.y, front.x))) < max(1.5, uViewRadius * 0.32)
                && p.y > uViewFloorY + 0.08) visibility = 0.15;
    }
    if (outside) {
        if (uViewFogStrength >= 1.0) discard;
        return 1.0 - uViewFogStrength;
    }
    return mix(1.0, visibility, uViewFogStrength);
}
void main() {
    float fog = applyViewFog(vWorld);
    float diffuse = max(dot(normalize(vNormal), -normalize(uLightDirection)), 0.0);
    vec3 color = vColor * (max(0.08, uAmbientIntensity) + uSunColor * diffuse * uLightIntensity * (1.0 - shadowAmount()));
    vec3 cell = vWorld - vNormal * 0.001;
    if (uSelected && all(greaterThanEqual(cell, uSelectedMin)) && all(lessThan(cell, uSelectedMin + vec3(0.25))))
        color = mix(color, vec3(1.0), 0.38);
    FragColor = vec4(color * fog, uAlpha);
}
