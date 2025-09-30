#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);
uniform int useTexture = 1;

void main() {
    if (useTexture == 0) {
        fragColor = tintColor;
    } else {
        vec4 textureColor = texture(textureSampler, fragTexCoord);
        fragColor = textureColor * tintColor;
    }
    
    if (fragColor.a < 0.1) {
        discard;
    }
}