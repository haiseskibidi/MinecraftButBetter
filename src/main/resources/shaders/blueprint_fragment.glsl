#version 330 core

in vec2 vPos;
out vec4 fragColor;

uniform vec4 tintColor = vec4(1.0, 1.0, 1.0, 1.0);
uniform int uType = 0; // 0: Matrix, 1: Ring, 2: Radial, 3: Rect, 4: Sonar, 5: Circle
uniform float uParam1 = 0.0;
uniform float uParam2 = 0.0;
uniform float uParam3 = 0.0;
uniform float uParam4 = 0.0; 
uniform float uTrigger0 = 0.0; 
uniform float uTime = 0.0;

void main() {
    if (uType == 0) {
        fragColor = tintColor;
        return;
    }

    vec2 p = vPos; // Ranges from -1.0 to 1.0
    float mask = 0.0;
    vec3 col = tintColor.rgb;

    if (uType == 1) { // RING
        float d = abs(length(p) - uParam1) - uParam2;
        mask = smoothstep(0.02, 0.0, d);
    } 
    else if (uType == 4) { // SONAR
        float d = length(p);
        float startR = uParam1; 
        if (d < startR) discard;

        float wave = sin((d - startR) * 20.0 - uTime * 6.0);
        mask = smoothstep(0.7, 1.0, wave);
        
        vec3 calmCol = vec3(0.0, 0.8, 1.0);
        vec3 alertCol = vec3(1.0, 0.9, 0.0);
        vec3 dangerCol = vec3(1.0, 0.1, 0.0);
        float colorFactor = clamp(uTrigger0 / 0.4, 0.0, 1.0);
        if (colorFactor < 0.5) col = mix(calmCol, alertCol, colorFactor * 2.0);
        else col = mix(alertCol, dangerCol, (colorFactor - 0.5) * 2.0);

        float maxReach = startR + 0.1 + colorFactor * 0.75;
        mask *= (1.0 - smoothstep(maxReach * 0.5, maxReach, d));
        mask *= (0.3 + colorFactor * 0.7);
    }
    else if (uType == 5) { // CIRCLE (Glow/Soft edge)
        float d = length(p);
        // Param1: radius, Param2: softness
        float softness = max(0.01, uParam2);
        mask = smoothstep(uParam1 + softness, uParam1 - softness, d);
    }

    if (mask < 0.01) discard;
    fragColor = vec4(col, tintColor.a * mask);
}
