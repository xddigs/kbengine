#version 330 core

in vec2 vTexCoord;

out vec4 FragColor;

uniform sampler2D uTexture;
uniform bool uUseTexture;
uniform bool uUseFont;
uniform bool uUseSilhouette;
uniform bool uPaperTool;

uniform bool uUseRoundedRect;
uniform vec2 uRectSize;
uniform float uCornerRadius;

uniform vec4 uColor;
uniform vec4 uBorderColor;
uniform float uBorderWidth;
uniform bool uBorderOnly;

float sdRoundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    if (uUseFont) {
        float alpha = texture(uTexture, vTexCoord).r;

        if (alpha <= 0.0) {
            discard;
        }

        FragColor = vec4(uColor.rgb, uColor.a * alpha);
        return;
    }

    vec4 finalColor = uColor;

    if (uUseTexture) {
        vec4 textureColor = texture(uTexture, vTexCoord);
        if (uUseSilhouette) {
            finalColor.a *= textureColor.a;
        } else {
            finalColor *= textureColor;
        }
    }

    if (uPaperTool) {
        finalColor.rgb *= vec3(0.62);
    }

    if (uUseRoundedRect) {
        vec2 halfSize = uRectSize * 0.5;
        vec2 p = (vTexCoord - vec2(0.5)) * uRectSize;

        float radius = clamp(
                uCornerRadius,
                0.0,
                min(halfSize.x, halfSize.y)
        );

        float dist = sdRoundedBox(p, halfSize, radius);
        float aa = fwidth(dist);

        float alpha = 1.0 - smoothstep(0.0, aa, dist);

        if (alpha <= 0.0) {
            discard;
        }

        if (uBorderWidth > 0.0) {
            float borderDist = dist + uBorderWidth;
            float borderAlpha = 1.0 - smoothstep(0.0, aa, borderDist);

            if (uBorderOnly) {
                if (dist + uBorderWidth <= 0.0) {
                    discard;
                }

                finalColor = uBorderColor;
            } else {
                finalColor = mix(
                        finalColor,
                        uBorderColor,
                        borderAlpha
                );
            }
        }

        finalColor.a *= alpha;
    }

    if (finalColor.a <= 0.0) {
        discard;
    }

    FragColor = finalColor;
}
