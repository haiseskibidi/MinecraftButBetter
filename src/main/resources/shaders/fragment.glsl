#version 330 core

in vec3 fragTexCoord;
in vec3 fragNormal;
in vec3 fragPos;
in float blockType;
in float neighborData;
in vec3 vLocalPos;
in float vBreakingIntensity;

out vec4 fragColor;

uniform sampler2DArray textureSampler;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 ambientLight;
uniform float glassLayer;   
uniform int highlightPass; // 1 = solid color mode, 0 = texture mode
uniform vec3 highlightColor;
uniform bool previewPass; 
uniform float previewAlpha;
uniform bool viewModelPass; 
uniform float brightnessMultiplier = 1.0;
uniform int faceMask = 0; // 16-bit mask for 4x4 grid
uniform bool useMask = false;
uniform float overlayLayer;
uniform float uWobbleTime;
uniform vec3 uHiddenBlockPos;
uniform bool uIsProxy;

uniform vec3 uCondition; // x=dirt, y=blood, z=wetness
uniform bool isHand = false;
uniform float uHandPartWeight = 0.0; // 1.0=hand, 0.6=forearm, 0.3=shoulder

// Modular Includes
#include "include/noise.glsl"
#include "include/hand_conditions.glsl"
#include "include/block_features.glsl"
#include "include/lighting.glsl"
#include "include/breaking_patterns.glsl"

void main() {
    if (!uIsProxy && uHiddenBlockPos.y >= 0.0) {
        vec3 push = fragNormal * 0.01;
        if (length(fragNormal) < 0.1) {
            push = vec3(0.0);
        }
        vec3 blockPos = floor(fragPos - push);
        if (distance(blockPos, uHiddenBlockPos) < 0.1) {
            discard;
        }
    }

    vec3 baseColor;    float alpha = 1.0;

    if (highlightPass != 0) {
        baseColor = highlightColor;
    } else {
        vec4 textureColor;
        if (useMask) {
            vec2 localUV = fragTexCoord.xy;
            int bit = int(clamp(localUV.y * 4.0, 0.0, 3.99)) * 4 + int(clamp(localUV.x * 4.0, 0.0, 3.99));
            if (((faceMask >> bit) & 1) == 0) discard;
            textureColor = texture(textureSampler, vec3(localUV, overlayLayer));
        } else {
            textureColor = texture(textureSampler, fragTexCoord);
        }

        // Handle specific block logic
        BlockInfo info = decodeBlockInfo(blockType);
        
        // Glass connectivity
        if (info.isGlass) {
            textureColor = applyGlassConnections(textureColor, fragTexCoord.xy, neighborData, fragTexCoord.z, textureSampler);
        }

        if (textureColor.a < 0.5) discard;

        baseColor = textureColor.rgb;
        alpha = textureColor.a;

        // Apply hand overlays (dirt, blood)
        if (isHand) {
            baseColor = applyHandConditions(baseColor, vLocalPos, uCondition, uHandPartWeight);
        }

        // Feature: Brighten Stump tops
        baseColor = brightenTopFace(baseColor, info.type, fragNormal);

        // Tinting (Leaves/Grass) - Apply after everything else to ensure color
        if (info.isTinted) {
            baseColor *= vec3(0.486, 0.784, 0.314);
        }

        // Apply Breaking Patterns
        if (uIsProxy && uBreakingProgress > 0.0) {
            baseColor = applyBreakingPattern(uBreakingPattern, baseColor, vLocalPos, uBreakingProgress);
        }
    }

    // Apply Lighting
    vec3 lighting = calculateLighting(fragNormal, lightDirection, lightColor, ambientLight);
    
    fragColor = vec4(lighting * baseColor * brightnessMultiplier, alpha);       

    if (previewPass) {
        fragColor.rgb = mix(fragColor.rgb, vec3(1.0), 0.3);
        fragColor.a *= previewAlpha;
    }
}
