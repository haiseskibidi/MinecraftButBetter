#version 330 core

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 displacement;

uniform vec4 scale;
uniform vec4 position_offset;

uniform float uHitPulse = 0.0; // Pulse on hit
uniform float uProgress = 0.0; // Mining progress [0..1]

uniform float uRecoilScale = 1.0; // Data-driven power of hit pulse
uniform float uSpreadScale = 0.0; // Data-driven power of progress spread

out vec2 vPos;
out vec2 vDisp;

void main() {
    vPos = position;
    vDisp = displacement;

    // Combined displacement logic:
    // 1. Recoil: sudden expansion on hit
    // 2. Spread: gradual expansion as block breaks
    float totalDisplacement = (uHitPulse * uRecoilScale) + (uProgress * uSpreadScale);
    
    vec2 finalPos = position + (displacement * totalDisplacement);

    gl_Position = vec4(finalPos * scale.xy + position_offset.xy, 0.0, 1.0);
}
