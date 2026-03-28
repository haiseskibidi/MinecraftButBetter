#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D screenTexture;
uniform vec2 screenSize;

void main() {
    vec2 texelSize = 1.0 / screenSize;
    vec3 result = vec3(0.0);
    
    // 4x4 Box Blur
    for (int x = -2; x < 2; x++) {
        for (int y = -2; y < 2; y++) {
            result += texture(screenTexture, fragTexCoord + vec2(x, y) * texelSize).rgb;
        }
    }
    
    fragColor = vec4(result / 16.0, 1.0);
}