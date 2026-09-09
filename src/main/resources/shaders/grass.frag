#version 330 core

in vec2 vTexCoord;
in vec3 vNormal;
in vec3 vFragPos;
in vec4 vLightSpacePosition;

out vec4 FragColor;

uniform sampler2D uTexture;
uniform sampler2D uShadowMap;
uniform vec4 uGrassTopUVBounds;
uniform vec4 uGrassSideUVBounds;
uniform vec3 uGrassTint;
uniform vec3 uSkyColor;
uniform vec3 uSunColor;
uniform float uLightIntensity;
uniform vec3 uLightDirection;
uniform float uAmbientIntensity;
uniform bool uEnableShadows;

uniform int uViewMode;
uniform vec3 uViewPlayerPosition;
uniform vec3 uViewCameraPosition;
uniform vec4 uViewBounds;
uniform float uViewRadius;
uniform float uViewFloorY;
uniform float uViewCeilingY;

const int MAX_TORCH_LIGHTS = 32;
uniform int uTorchCount;
uniform vec3 uTorchPositions[MAX_TORCH_LIGHTS];
uniform int uShadowedTorchCount;
uniform samplerCube uTorchShadowMaps[4];
uniform float uTorchShadowFarPlane;

bool isInside(vec2 uv, vec4 bounds) {
    return uv.x >= bounds.x && uv.x <= bounds.z
            && uv.y >= bounds.y && uv.y <= bounds.w;
}

float applyViewFog(vec3 worldPosition) {
    if (uViewMode == 0) return 1.0;
    vec2 offset = worldPosition.xz - uViewPlayerPosition.xz;
    vec2 cameraOffset = uViewCameraPosition.xz - uViewPlayerPosition.xz;
    vec2 toCamera = length(cameraOffset) > 0.001
            ? normalize(cameraOffset) : vec2(0.7071, 0.7071);

    if (uViewMode == 1) {
        if (worldPosition.x < uViewBounds.x || worldPosition.z < uViewBounds.y
                || worldPosition.x > uViewBounds.z || worldPosition.z > uViewBounds.w
                || worldPosition.y >= uViewCeilingY - 0.02) discard;
        bool frontWall = (toCamera.x > 0.15 && worldPosition.x > uViewBounds.z - 1.01)
                || (toCamera.x < -0.15 && worldPosition.x < uViewBounds.x + 1.01)
                || (toCamera.y > 0.15 && worldPosition.z > uViewBounds.w - 1.01)
                || (toCamera.y < -0.15 && worldPosition.z < uViewBounds.y + 1.01);
        if (frontWall && worldPosition.y > uViewFloorY + 0.08) discard;
        float edge = min(min(worldPosition.x - uViewBounds.x,
                             uViewBounds.z - worldPosition.x),
                         min(worldPosition.z - uViewBounds.y,
                             uViewBounds.w - worldPosition.z));
        return mix(0.35, 1.0, smoothstep(0.0, 1.25, edge));
    }

    float distanceFromPlayer = length(offset);
    if (distanceFromPlayer > uViewRadius || worldPosition.y >= uViewCeilingY) discard;
    float frontDepth = dot(offset, toCamera);
    float sideDistance = abs(dot(offset, vec2(-toCamera.y, toCamera.x)));
    if (frontDepth > 0.20 && sideDistance < max(1.5, uViewRadius * 0.32)
            && worldPosition.y > uViewFloorY + 0.08) discard;
    return 1.0 - smoothstep(max(0.0, uViewRadius - 2.0),
                            uViewRadius, distanceFromPlayer);
}

float calculateShadow(vec4 lightSpacePosition, vec3 normal) {
    vec3 coordinates = lightSpacePosition.xyz / lightSpacePosition.w;
    coordinates = coordinates * 0.5 + 0.5;
    if (coordinates.x < 0.0 || coordinates.x > 1.0 || coordinates.y < 0.0
            || coordinates.y > 1.0 || coordinates.z < 0.0 || coordinates.z > 1.0) return 0.0;

    float bias = max(0.002, 0.008 * (1.0 - max(dot(normal, normalize(-uLightDirection)), 0.0)));
    vec2 texelSize = 1.0 / vec2(textureSize(uShadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closest = texture(uShadowMap, coordinates.xy + vec2(x, y) * texelSize).r;
            shadow += coordinates.z - bias > closest ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

float torchShadowDepth(int index, vec3 lightToFragment) {
    if (index == 0) return texture(uTorchShadowMaps[0], lightToFragment).r;
    if (index == 1) return texture(uTorchShadowMaps[1], lightToFragment).r;
    if (index == 2) return texture(uTorchShadowMaps[2], lightToFragment).r;
    return texture(uTorchShadowMaps[3], lightToFragment).r;
}

void main() {
    float viewFog = applyViewFog(vFragPos);
    if (!isInside(vTexCoord, uGrassTopUVBounds) && !isInside(vTexCoord, uGrassSideUVBounds)) discard;

    vec4 texColor = texture(uTexture, vTexCoord);
    bool isGreenGrassPixel = texColor.g > texColor.r * 1.05 && texColor.g > texColor.b * 1.05;
    if (texColor.a < 0.01 || !isGreenGrassPixel) discard;

    vec3 normal = normalize(vNormal);
    if (!gl_FrontFacing) normal = -normal;
    float diffuse = max(dot(normal, normalize(-uLightDirection)), 0.0);
    float shadow = uEnableShadows ? calculateShadow(vLightSpacePosition, normal) : 0.0;
    vec3 light = uSkyColor * uAmbientIntensity
            + uSunColor * diffuse * uLightIntensity * (1.0 - shadow);
    vec3 torchLight = vec3(0.0);
    for (int i = 0; i < uTorchCount; i++) {
        float distanceToTorch = distance(vFragPos, uTorchPositions[i]);
        float attenuation = max(0.0, 1.0 - distanceToTorch / 8.0);
        float pointShadow = 0.0;
        if (i < uShadowedTorchCount && distanceToTorch > 0.12) {
            vec3 lightToFragment = vFragPos - uTorchPositions[i];
            float closestDepth = torchShadowDepth(i, lightToFragment)
                    * uTorchShadowFarPlane;
            float normalDotLight = max(dot(normal, normalize(-lightToFragment)), 0.0);
            float bias = max(0.025, 0.10 * (1.0 - normalDotLight));
            pointShadow = smoothstep(-0.035, 0.035,
                    distanceToTorch - bias - closestDepth);
        }
        torchLight += vec3(1.0, 0.62, 0.24) * attenuation * attenuation
                * (1.0 - pointShadow);
    }
    FragColor = vec4(texColor.rgb * uGrassTint * (light + torchLight) * viewFog, texColor.a);
}
