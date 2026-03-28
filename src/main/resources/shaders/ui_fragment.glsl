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

// Slot Shape parameters
uniform int isSlot = 0; // 1 if we are rendering a slot background
uniform float cornerRadius = 0.15; // Chamfer size

// SDF for a chamfered rectangle
float sdChamferedRect(vec2 p, vec2 b, float r) {
    p = abs(p) - b;
    return length(max(p, 0.0)) + min(max(p.x, p.y), 0.0) - r;
}

void main() {
    if (isSlot == 1) {
        // Transform UV to -1..1 range
        vec2 p = fragTexCoord * 2.0 - 1.0;
        
        // Calculate SDF for industrial octagon
        float d = sdChamferedRect(p, vec2(0.85), cornerRadius);
        
        // Hard edge for the shape
        float mask = smoothstep(0.01, 0.0, d);
        
        // Subtle inner glow/border
        float border = smoothstep(0.05, 0.0, abs(d));
        
        vec3 finalColor = tintColor.rgb;
        finalColor += border * 0.15; // Light highlight on edges
        
        fragColor = vec4(finalColor, tintColor.a * mask);
        
        if (fragColor.a < 0.01) discard;
        return;
    }

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
