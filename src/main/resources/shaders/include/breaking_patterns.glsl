// Procedural Material Breaking Patterns

vec3 applyWoodNotch(vec3 color, vec3 localPos, vec3 notchPos, float progress, bool isFresh) {
    // Localize the effect around the notch position
    float dist = distance(localPos, notchPos);
    if (dist > 0.25) return color; 

    // Seed based on notch position for uniqueness
    float seed = hash3D(notchPos * 10.0);
    
    // Pixelated coordinates
    vec3 pixelPos = floor(localPos * 16.0);
    
    // Jagged horizontal cut
    float jagged = floor(noise(localPos.xz * 10.0 + seed) * 2.0);
    float cutY = floor(notchPos.y * 16.0) + jagged;
    
    // V-shaped notch mask
    float horizDist = distance(localPos.xz, notchPos.xz);
    float range = (1.0 + progress * 2.0) * smoothstep(0.25, 0.0, horizDist);
    
    if (range > 0.0 && abs(pixelPos.y - cutY) < range) {
        float vDist = abs(pixelPos.y - cutY);
        float intensity = (1.0 - (vDist / range)) * smoothstep(0.25, 0.0, horizDist);
        
        if (isFresh) {
            // Bright white fresh cut
            return mix(color, vec3(1.0, 1.0, 1.0), intensity * 0.9);
        } else {
            // Dark damaged old cut
            return mix(color, color * 0.15, intensity * 0.8);
        }
    }
    
    return color;
}

vec3 applyWeakSpotMarker(vec3 color, vec3 localPos, vec3 weakSpotPos, float progress) {
    // We now use applyWoodNotch for the marker itself if it's wood, 
    // but we can keep a subtle glow for the "target" part.
    float dist = distance(localPos, weakSpotPos);
    float qDist = floor(dist * 16.0) / 16.0;
    
    if (qDist < 0.08) {
        float pulse = sin(uWobbleTime * 20.0) * 0.5 + 0.5;
        vec3 glowColor = vec3(1.0, 0.9, 0.4);
        return mix(color, glowColor, (0.4 + pulse * 0.4) * smoothstep(0.08, 0.0, qDist));
    }
    return color;
}

vec3 applyBreakingPattern(int patternId, vec3 color, vec3 localPos, float progress) {
    if (progress <= 0.0) return color;
    
    vec3 result = color;
    
    if (patternId == 1) { // WOOD
        // 1. Render all previous hits from history (Old/Dark)
        for (int i = 0; i < uHitCount; i++) {
            if (i < 16) {
                result = applyWoodNotch(result, localPos, uHitHistory[i], progress, false);
            }
        }
        // 2. Render current target (Fresh/White)
        result = applyWoodNotch(result, localPos, uWeakSpotPos, progress, true);
    }
    
    return result;
}
