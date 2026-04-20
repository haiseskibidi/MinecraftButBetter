#version 330 core

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 displacement;

uniform vec4 scale;
uniform vec4 position_offset;

uniform float uTrigger0 = 0.0;
uniform float uTrigger1 = 0.0;
uniform float uTrigger2 = 0.0;
uniform float uTrigger3 = 0.0;
uniform float uTime = 0.0;

uniform int uTriggerSlot = 0;
uniform float uIntensity = 1.0;
uniform int uAnimType = 0; // 0: None, 1: Expand, 2: Pulse, 3: Rotate, 4: Shake

out vec2 vPos;

void main() {
    vPos = position; // Quad is [-1, 1], perfect for SDF
    
    float trigger = 0.0;
    if (uTriggerSlot == 0) trigger = uTrigger0;
    else if (uTriggerSlot == 1) trigger = uTrigger1;
    else if (uTriggerSlot == 2) trigger = uTrigger2;
    else if (uTriggerSlot == 3) trigger = uTrigger3;

    vec2 animatedPos = position;

    if (uAnimType == 1) { // Expand
        animatedPos += displacement * (trigger * uIntensity * 0.5);
    } else if (uAnimType == 2) { // Pulse
        float pulse = sin(uTime * 5.0) * 0.1 * trigger * uIntensity;
        animatedPos += displacement * (pulse * 2.0);
    } else if (uAnimType == 4) { // SHAKE
        float freq = 60.0;
        float s = sin(uTime * freq) * trigger * uIntensity * 0.1;
        float c = cos(uTime * freq * 1.1) * trigger * uIntensity * 0.1;
        animatedPos += vec2(s, c);
    }

    gl_Position = vec4(animatedPos * scale.xy + position_offset.xy, 0.0, 1.0);
}
