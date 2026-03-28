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

// Slot Shape & Light parameters
uniform int isSlot = 0; 
uniform float cornerRadius = 0.15;
uniform float hoverProgress = 0.0; // 0.0 to 1.0

// SDF for a chamfered rectangle
float sdChamferedRect(vec2 p, vec2 b, float r) {
    p = abs(p) - b;
    return length(max(p, 0.0)) + min(max(p.x, p.y), 0.0) - r;
}

void main() {
    if (isSlot == 1) {
        vec2 p = fragTexCoord * 2.0 - 1.0;
        
        // 1. Calculate Shape Mask
        float d = sdChamferedRect(p, vec2(0.85), cornerRadius);
        float mask = smoothstep(0.01, 0.0, d);
        if (mask < 0.01) discard;

        // 2. Base Color (Dark industrial grey)
        vec3 baseColor = tintColor.rgb;
        
        // 3. Central Ambient Light (Inner Glow)
        // Makes center brighter so dark items pop
        float distFromCenter = length(p);
        float innerGlow = exp(-distFromCenter * 1.5) * 0.25;
        
        // 4. Edge Highlight (Bevel effect)
        float border = smoothstep(0.05, 0.0, abs(d));
        
        // 5. Dynamic Hover Glow
        // When hovered, the slot "wakes up" with a subtle cyan/blue tint or just more light
        float interactionLight = hoverProgress * 0.1;
        
        vec3 finalColor = baseColor;
        finalColor += innerGlow; // Light behind the item
        finalColor += border * (0.1 + hoverProgress * 0.2); // Brighter edges on hover
        finalColor += interactionLight;
        
        fragColor = vec4(finalColor, tintColor.a * mask);
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
