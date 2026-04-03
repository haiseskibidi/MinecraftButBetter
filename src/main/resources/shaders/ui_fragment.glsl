#version 330 core

in vec2 fragTexCoord;
in vec2 vPos;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform sampler2DArray arraySampler;
uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);
uniform int useTexture = 1;
uniform int useArray = 0;
uniform float layerIndex = 0.0;
uniform int isGrayscale = 0;

// Mining / Action Progress
uniform float uProgress = 0.0;
uniform int isCrosshair = 0;

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
        float d = sdChamferedRect(p, vec2(0.85), cornerRadius);
        float mask = smoothstep(0.01, 0.0, d);
        if (mask < 0.01) discard;

        vec3 baseColor = tintColor.rgb;
        float distFromCenter = length(p);
        float innerGlow = exp(-distFromCenter * 1.5) * 0.25;
        float border = smoothstep(0.05, 0.0, abs(d));
        float interactionLight = hoverProgress * 0.1;
        
        vec3 finalColor = baseColor + innerGlow + border * (0.1 + hoverProgress * 0.2) + interactionLight;
        fragColor = vec4(finalColor, tintColor.a * mask);
        return;
    }

    if (useTexture == 0) {
        vec4 finalColor = tintColor;
        
        if (isCrosshair == 1 && uProgress > 0.001) {
            // Enhanced Multi-stage color ramp for Mining
            vec3 white = tintColor.rgb;
            vec3 orange = vec3(1.0, 0.55, 0.0);
            vec3 softRed = vec3(1.0, 0.25, 0.25);
            
            vec3 midColor;
            if (uProgress < 0.5) {
                midColor = mix(white, orange, uProgress * 2.0);
            } else {
                midColor = mix(orange, softRed, (uProgress - 0.5) * 2.0);
            }
            
            finalColor.rgb = midColor;
            
            // Subtle "strain" pulse - high frequency, low amplitude
            float pulse = 1.0 + sin(uProgress * 30.0) * 0.12 * uProgress;
            finalColor.rgb *= pulse;
            
            // Slight glow boost at the end
            finalColor.rgb += 0.15 * smoothstep(0.7, 1.0, uProgress);
        }
        
        fragColor = finalColor;
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
