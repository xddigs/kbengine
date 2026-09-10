#version 330 core

in vec3 vNormal;
in vec2 vTexCoord;
in vec3 vFragPos;
in vec4 vLightSpacePosition;
in float vIsWater;

out vec4 FragColor;

uniform sampler2D uTexture;
uniform vec4 uWaterUVBounds;
uniform sampler2D uShadowMap;

uniform bool uUseTexture;
uniform vec3 uBaseColor;
uniform bool uUseFaceAtlas;
uniform bool uUseColorTint;
uniform vec3 uColorTint;

uniform vec3 uSkyColor;
uniform vec3 uSunColor;
uniform float uLightIntensity;
uniform vec3 uLightDirection;
uniform float uAmbientIntensity;

const int MAX_TORCH_LIGHTS = 32;
uniform int uTorchCount;
uniform vec3 uTorchPositions[MAX_TORCH_LIGHTS];
uniform bool uIsTorch;
uniform int uShadowedTorchCount;
uniform samplerCube uTorchShadowMaps[4];
uniform float uTorchShadowFarPlane;

uniform bool uIsMaskPass;
uniform float uParticleAlpha;
uniform bool uEnableShadows;
uniform bool uIsSprite;
uniform bool uIsSubmergedEntity;
uniform bool uIsWater;
uniform vec4 uLavaUVBounds;

uniform int uViewMode;
uniform vec3 uViewPlayerPosition;
uniform vec3 uViewCameraPosition;
uniform vec4 uViewBounds;
uniform float uViewRadius;
uniform float uViewFloorY;
uniform float uViewCeilingY;
uniform float uViewFogStrength;
uniform bool uIgnoreViewFog;
uniform bool uVoxelBreakActive;
uniform vec3 uVoxelBreakPosition;
uniform bool uModelBreakActive;
uniform float uModelBreakProgress;

float breakCell(vec3 position) {
    vec3 cell = floor(position * vec3(8.0, 4.0, 8.0));
    return fract(sin(dot(cell, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
}

float applyViewFog(vec3 worldPosition) {
    if (uIgnoreViewFog || uViewMode == 0 || uViewFogStrength <= 0.0) return 1.0;

    vec2 offset = worldPosition.xz - uViewPlayerPosition.xz;
    vec2 cameraOffset = uViewCameraPosition.xz - uViewPlayerPosition.xz;
    vec2 toCamera = length(cameraOffset) > 0.001
            ? normalize(cameraOffset) : vec2(0.7071, 0.7071);

    if (uViewMode == 1) {
        if (worldPosition.x < uViewBounds.x || worldPosition.z < uViewBounds.y
                || worldPosition.x > uViewBounds.z || worldPosition.z > uViewBounds.w) {
            if (uViewFogStrength >= 1.0) discard;
            return 1.0 - uViewFogStrength;
        }
        if (worldPosition.y >= uViewCeilingY - 0.02) {
            if (uViewFogStrength >= 1.0) discard;
            return 1.0 - uViewFogStrength;
        }

        bool frontWall = (toCamera.x > 0.15 && worldPosition.x > uViewBounds.z - 1.01)
                || (toCamera.x < -0.15 && worldPosition.x < uViewBounds.x + 1.01)
                || (toCamera.y > 0.15 && worldPosition.z > uViewBounds.w - 1.01)
                || (toCamera.y < -0.15 && worldPosition.z < uViewBounds.y + 1.01);
        if (frontWall && worldPosition.y > uViewFloorY + 0.08) {
            if (uViewFogStrength >= 1.0) discard;
            return 1.0 - uViewFogStrength;
        }

        float edge = min(min(worldPosition.x - uViewBounds.x,
                             uViewBounds.z - worldPosition.x),
                         min(worldPosition.z - uViewBounds.y,
                             uViewBounds.w - worldPosition.z));
        return mix(1.0, mix(0.35, 1.0, smoothstep(0.0, 1.25, edge)), uViewFogStrength);
    }

    float distanceFromPlayer = length(offset);
    if (distanceFromPlayer > uViewRadius || worldPosition.y >= uViewCeilingY) {
        if (uViewFogStrength >= 1.0) discard;
        return 1.0 - uViewFogStrength;
    }

    float frontDepth = dot(offset, toCamera);
    float sideDistance = abs(dot(offset, vec2(-toCamera.y, toCamera.x)));
    float corridorWidth = max(1.5, uViewRadius * 0.32);

    if (frontDepth > 0.20 && sideDistance < corridorWidth
        && worldPosition.y > uViewFloorY + 0.08) {
        return mix(1.0, 0.15, uViewFogStrength);
    }

    return mix(1.0, 1.0 - smoothstep(max(0.0, uViewRadius - 2.0),
                            uViewRadius, distanceFromPlayer), uViewFogStrength);
}

float calculateShadow(vec4 lightSpacePosition, vec3 normal) {
    vec3 projectionCoordinates = lightSpacePosition.xyz / lightSpacePosition.w;
    projectionCoordinates = projectionCoordinates * 0.5 + 0.5;

    if (projectionCoordinates.x < 0.0 || projectionCoordinates.x > 1.0 ||
        projectionCoordinates.y < 0.0 || projectionCoordinates.y > 1.0 ||
        projectionCoordinates.z < 0.0 || projectionCoordinates.z > 1.0) {
        return 0.0;
    }

    vec3 lightDir = normalize(-uLightDirection);
    float normalDotLight = dot(normal, lightDir);

    if (normalDotLight <= 0.0) {
        return 1.0;
    }

    float bias = max(0.0015 * (1.0 - normalDotLight), 0.0005);
    float currentDepth = projectionCoordinates.z;
    vec2 texelSize = 1.0 / vec2(textureSize(uShadowMap, 0));

    float shadow = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float closestDepth = texture(uShadowMap, projectionCoordinates.xy + vec2(x, y) * texelSize).r;
            shadow += (currentDepth - bias > closestDepth) ? 1.0 : 0.0;
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
    vec3 blockSample = vFragPos - normalize(vNormal) * 0.001;
    if (uVoxelBreakActive
            && all(greaterThanEqual(blockSample, uVoxelBreakPosition))
            && all(lessThan(blockSample, uVoxelBreakPosition + vec3(1.0)))) {
        discard;
    }
    if (uModelBreakActive && breakCell(vFragPos) < uModelBreakProgress) {
        discard;
    }
    float viewFog = applyViewFog(vFragPos);
    vec4 texColor = vec4(uBaseColor, 1.0);

    if (uUseTexture) {
        vec2 sampleUV = vTexCoord;
        bool isWaterSurface = uIsWater && abs(normalize(vNormal).y) > 0.5 &&
                              vTexCoord.x >= uWaterUVBounds.x && vTexCoord.x <= uWaterUVBounds.z &&
                              vTexCoord.y >= uWaterUVBounds.y && vTexCoord.y <= uWaterUVBounds.w;
        if (isWaterSurface) {
            sampleUV = mix(uWaterUVBounds.xy, uWaterUVBounds.zw, fract(vFragPos.xz));
        }
        texColor = texture(uTexture, sampleUV);
        if (texColor.a < 0.01 && !uIsWater) {
            discard;
        }
    }

    if (uIsMaskPass) {
        FragColor = vec4(1.0);
        return;
    }

    vec3 normal = normalize(vNormal);
    if (!gl_FrontFacing) {
        normal = -normal;
    }

    vec3 lightDir = normalize(-uLightDirection);
    float diffuse = max(dot(normal, lightDir), 0.0);

    if (uIsSprite) {
        diffuse = 0.95;
    }

    float shadow = 0.0;
    if (uEnableShadows && !uIsSprite) {
        shadow = calculateShadow(vLightSpacePosition, normal);
    }

    vec3 ambient = uSkyColor * uAmbientIntensity;
    vec3 directLight = uSunColor * diffuse * uLightIntensity * (1.0 - shadow);
    vec3 torchLight = vec3(0.0);

    for (int i = 0; i < uTorchCount; i++) {
        float distanceToTorch = distance(vFragPos, uTorchPositions[i]);
        float attenuation = max(0.0, 1.0 - distanceToTorch / 10.0);
        float pointShadow = 0.0;

        if (i < uShadowedTorchCount && distanceToTorch > 0.12) {
            vec3 lightToFragment = vFragPos - uTorchPositions[i];
            float closestDepth = torchShadowDepth(i, lightToFragment) * uTorchShadowFarPlane;
            float normalDotLight = max(dot(normal, normalize(-lightToFragment)), 0.0);
            float bias = max(0.025, 0.10 * (1.0 - normalDotLight));
            pointShadow = smoothstep(-0.035, 0.035,
                    distanceToTorch - bias - closestDepth);
        }

        torchLight += vec3(1.0, 0.62, 0.24) * attenuation * 0.4 * (1.0 - pointShadow);
    }

    vec3 totalLight = ambient + directLight + torchLight;
    if (uIsTorch) totalLight = max(totalLight, vec3(1.0, 0.62, 0.24));
    float alpha = texColor.a * uParticleAlpha;
    vec3 tint = uUseColorTint ? uColorTint : vec3(1.0);
    vec3 finalColor = texColor.rgb * tint * totalLight * viewFog;

    if (vIsWater > 0.5 && uIsWater) {
        bool isLava = vTexCoord.x > uLavaUVBounds.x && vTexCoord.x < uLavaUVBounds.z &&
                      vTexCoord.y > uLavaUVBounds.y && vTexCoord.y < uLavaUVBounds.w;
        alpha = isLava ? 1.0 : 0.80;
    }

    if (uIsSubmergedEntity) {
        finalColor *= vec3(0.65, 0.85, 1.0);
        alpha = uParticleAlpha;
    }

    FragColor = vec4(finalColor, alpha);
}
