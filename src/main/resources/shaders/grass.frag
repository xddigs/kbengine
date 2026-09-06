#version 330 core

in vec2 vTexCoord;
in vec3 vNormal;
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

bool isInside(vec2 uv, vec4 bounds) {
    return uv.x >= bounds.x && uv.x <= bounds.z
            && uv.y >= bounds.y && uv.y <= bounds.w;
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

void main() {
    // UV filtering confines this pass to GRASS, not every green texture in the atlas.
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
    FragColor = vec4(texColor.rgb * uGrassTint * light, texColor.a);
}
