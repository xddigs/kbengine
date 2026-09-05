#version 330 core

in vec2 vTexCoord;

out vec4 FragColor;

uniform sampler2D uTexture;

void main() {
    vec4 crack = texture(uTexture, vTexCoord);
    if (crack.a < 0.01) {
        discard;
    }
    FragColor = crack;
}
