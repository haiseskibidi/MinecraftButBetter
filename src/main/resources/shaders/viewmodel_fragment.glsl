#version 330 core

in vec3 fragTexCoord;
in vec3 fragNormal;
in vec3 fragPos;
in vec3 vLocalPos;
in float blockType;

out vec4 fragColor;

uniform sampler2DArray textureSampler;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 ambientLight;

uniform vec3 uCondition; // x=dirt, y=blood, z=wetness
uniform bool isHand = false;
uniform float uHandPartWeight = 0.0; // 1.0=hand, 0.6=forearm, 0.3=shoulder

uniform float uMiningHeat = 0.0; // 0.0 to 1.0 intensity
uniform float uTime;
uniform float uAlpha = 1.0;

// Modular Includes
#include "include/noise.glsl"
#include "include/hand_conditions.glsl"
#include "include/block_features.glsl"
#include "include/lighting.glsl"

void main() {
    vec4 textureColor = texture(textureSampler, fragTexCoord);
    if (textureColor.a < 0.5) discard;

    vec3 baseColor = textureColor.rgb;
    float alpha = textureColor.a;

    BlockInfo info = decodeBlockInfo(blockType);

    // 1. Apply Hand Conditions (Dirt, Blood)
    if (isHand) {
        baseColor = applyHandConditions(baseColor, vLocalPos, uCondition, uHandPartWeight);
    }

    // 2. Mining Heat Logic (Subtle Hot Metal Look)
    float glowMask = 0.0;
    if (isHand) {
        glowMask = smoothstep(0.85, 1.0, uHandPartWeight);
    } else {
        // Starts just above the handle, full at the tip
        glowMask = smoothstep(-0.2, 0.4, vLocalPos.y);
    }

    if (uMiningHeat > 0.01) {
        float h = glowMask * uMiningHeat;
        vec3 hotRed = vec3(1.0, 0.05, 0.0); // Slightly more natural hot red
        
        // Subtle pulse (range 0.95 - 1.05)
        float pulse = (0.95 + 0.05 * sin(uTime * 20.0)) * h;
        
        // 2.1 Tint the base texture (keeps details visible)
        baseColor = mix(baseColor, hotRed, h * 0.6);
        
        // 2.2 Add subtle emission (doesn't blind the player)
        baseColor += hotRed * pulse * 0.7; 
    }

    // 3. Apply Lighting
    vec3 lighting = calculateLighting(fragNormal, lightDirection, lightColor, ambientLight);
    
    // 4. Tinting (Leaves/Grass)
    if (info.isTinted) {
        baseColor *= vec3(0.486, 0.784, 0.314);
    }

    fragColor = vec4(lighting * baseColor, alpha * uAlpha);
}
