#version 330 core

layout(location = 0) in vec2 position;
layout(location = 1) in vec2 texCoord; // Used as UV for most UI, but as Displacement for Crosshairs

uniform vec4 scale;    // scaleX, scaleY, unused, unused
uniform vec4 position_offset; // offsetX, offsetY, unused, unused
uniform vec4 uvOffset; // uvX, uvY, unused, unused
uniform vec4 uvScale;  // uvScaleX, uvScaleY, unused, unused

// Mining / Action Progress
uniform float uProgress = 0.0;
uniform float uHitPulse = 0.0; // 0 to 1 recoil on hit
uniform int isCrosshair = 0; // 0: None, 1: Standard, 2: Halo Ring

out vec2 fragTexCoord;
out vec2 vPos;

void main() {
    vPos = position;
    vec2 finalPos = position;

    // --- CROSSHAIR RECOIL (Expansion) ---
    // For crosshairs, the texCoord attribute (location 1) contains pre-calculated 
    // per-pixel displacement vectors to ensure they move as solid units.
    if (isCrosshair == 1 && uHitPulse > 0.001) {
        float recoilFactor = uHitPulse * 6.0; 
        finalPos += texCoord * recoilFactor;
    }
    
    // Apply scale and offset
    vec2 scaledPos = finalPos * scale.xy + position_offset.xy;
    
    // CRITICAL FIX: Restore proper UV mapping for all UI elements (Fonts, Items, Icons)
    // The previous hardcoded 'position * 0.5 + 0.5' flipped items and broke font rendering.
    fragTexCoord = texCoord * uvScale.xy + uvOffset.xy;
    
    gl_Position = vec4(scaledPos, 0.0, 1.0);
}
