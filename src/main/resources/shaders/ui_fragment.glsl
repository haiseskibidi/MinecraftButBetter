#version 330 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform sampler2DArray arraySampler;
uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);
uniform int useTexture = 1;
uniform int useArray = 0;
uniform float layerIndex = 0.0;
uniform int isGrayscale = 0;

void main() {
    if (useTexture == 0) {
        fragColor = tintColor;
    } else {
        vec4 textureColor;
        if (useArray == 1) {
            textureColor = texture(arraySampler, vec3(fragTexCoord, layerIndex));
        } else {
            textureColor = texture(textureSampler, fragTexCoord);
        }
        
        if (isGrayscale == 1) {
            float gray = dot(textureColor.rgb, vec3(0.299, 0.587, 0.114));
            textureColor.rgb = vec3(gray);
        }
        
        fragColor = textureColor * tintColor;
    }
    
    if (fragColor.a < 0.1) {
        discard;
    }
}