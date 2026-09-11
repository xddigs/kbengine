#version 330 core

in vec2 vTexCoord;

out vec4 FragColor;

uniform sampler2D uScene;

const vec3 PAPER_WHITE = vec3(1.00, 0.95, 0.85);
const vec3 PAPER_LIGHT = vec3(0.78, 0.77, 0.73);
const vec3 PAPER_DARK = vec3(0.45, 0.45, 0.43);
const vec3 INK_BLACK = vec3(0.055, 0.055, 0.05);

void main() {
    vec4 source = texture(uScene, vTexCoord);
    float luminance = dot(source.rgb, vec3(0.299, 0.587, 0.114));

    vec3 paperTone;
    if (luminance < 0.16) {
        paperTone = INK_BLACK;
    } else if (luminance < 0.36) {
        paperTone = PAPER_DARK;
    } else if (luminance < 0.60) {
        paperTone = PAPER_LIGHT;
    } else {
        paperTone = PAPER_WHITE;
    }

    FragColor = vec4(paperTone, source.a);
}
