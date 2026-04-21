#version 330 core

in vec2 fragTexCoord;
in vec2 vPos;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform sampler2DArray arraySampler;
uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);
uniform vec4 tintColor2 = vec4(1.0, 1.0, 1.0, 0.0); // Second color for gradients
uniform int useTexture = 1;
uniform int useArray = 0;
uniform float layerIndex = 0.0;
uniform int isGrayscale = 0;
uniform int isGradient = 0; // 0: None, 1: Horizontal

// Mining / Action Progress
uniform float uProgress = 0.0;
uniform int isCrosshair = 0; // 0: None, 1: Standard, 2: Halo Ring

// Slot Shape & Light parameters
uniform int isSlot = 0; 
uniform float cornerRadius = 0.15;
uniform float hoverProgress = 0.0; // 0.0 to 1.0

// Sensory Iris (Noise Indicator)
uniform int isIris = 0;
uniform float uNoise = 0.0;
uniform float uTime = 0.0;

// SDF for a chamfered rectangle
float sdChamferedRect(vec2 p, vec2 b, float r) {
    p = abs(p) - b;
    return length(max(p, 0.0)) + min(max(p.x, p.y), 0.0) - r;
}

void main() {
    if (isIris == 1) {
        vec2 uv = fragTexCoord * 2.0 - 1.0;
        float dist = length(uv);
        float angle = atan(uv.y, uv.x);
        
        // 1. Procedural Heartbeat & Base Scale
        float heartbeat = sin(uTime * 3.0) * 0.04 * (1.0 - uNoise);
        float baseRadius = 0.25 + heartbeat;
        
        // 2. Central Ring
        float ring = smoothstep(0.03, 0.0, abs(dist - baseRadius));
        
        // 3. Sensory Needles (Spikes)
        float n = 36.0; 
        float angleStep = 6.28318 / n;
        float a = floor(angle / angleStep + 0.5) * angleStep;
        
        // Jitter & Reactivity
        float jitter = (sin(uTime * 60.0 + a * 100.0) * 0.03) * uNoise;
        float spikeMask = smoothstep(0.04, 0.0, abs(angle - a));
        
        float targetLen = baseRadius + (uNoise * 0.65) + jitter;
        float spikes = step(dist, targetLen) * step(baseRadius, dist) * spikeMask;
        
        // Subtle center glow
        float glow = exp(-dist * 3.0) * 0.3 * (uNoise + 0.5);
        
        // 4. Adaptive Coloring
        vec3 calmCol = vec3(0.0, 0.9, 1.0);
        vec3 alertCol = vec3(1.0, 0.8, 0.0);
        vec3 dangerCol = vec3(1.0, 0.0, 0.1);
        
        vec3 col;
        if (uNoise < 0.4) col = mix(calmCol, alertCol, uNoise / 0.4);
        else col = mix(alertCol, dangerCol, (uNoise - 0.4) / 0.6);
        
        float finalAlpha = clamp(ring + spikes + glow, 0.0, 1.0);
        fragColor = vec4(col, finalAlpha * tintColor.a);
        return;
    }

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
        
        if (isGradient == 1) {
            // Smooth S-curve for "soft" feel
            float edge = smoothstep(0.0, 1.0, fragTexCoord.x);
            finalColor = mix(tintColor, tintColor2, edge);
        }
        
        if (isCrosshair == 1 && uProgress > 0.001) {
            // Subtle color change towards the end of mining
            vec3 focusColor = mix(vec3(1.0), vec3(1.0, 0.8, 0.4), uProgress);
            finalColor.rgb = focusColor;
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
    
    if (fragColor.a < 0.005) {
        discard;
    }
}
