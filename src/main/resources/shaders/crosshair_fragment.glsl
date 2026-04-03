#version 330 core

in vec2 vPos;
out vec4 fragColor;

uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);

void main() {
    fragColor = tintColor;
}
