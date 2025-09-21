#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;

void main() {
    vec4 textureColor = texture(textureSampler, fragTexCoord);
    
    // Пропускаем полностью прозрачные пиксели
    if (textureColor.a < 0.1) {
        discard;
    }
    
    fragColor = textureColor;
}