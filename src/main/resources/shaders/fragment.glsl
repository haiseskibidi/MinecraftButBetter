#version 330 core

in vec4 fragTexCoord;
in vec3 fragNormal;
in vec3 fragPos;
in float blockType;
in float neighborData;
in vec3 vLocalPos;
in float vBreakingIntensity;
in vec2 vLight;
in float vAO;

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
uniform vec3 uGrassColor = vec3(0.486, 0.784, 0.314);

uniform vec3 uCondition; // x=dirt, y=blood, z=wetness
uniform bool isHand = false;
uniform float uHandPartWeight = 0.0; // 1.0=hand, 0.6=forearm, 0.3=shoulder

uniform vec3 uPlayerLightPos;
uniform float uPlayerLightLevel; // 0.0 to 15.0

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
            textureColor = texture(textureSampler, fragTexCoord.xyz);
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

        // Unified Tinting (Leaves/Grass)
        if (info.isTinted) {
            if (fragTexCoord.w >= 0.0 && !info.isGlass) {
                vec4 overlayTex = texture(textureSampler, vec3(fragTexCoord.xy, fragTexCoord.w));
                if (overlayTex.a > 0.1) {
                    baseColor = mix(baseColor, overlayTex.rgb * uGrassColor, overlayTex.a);
                }
            } else {
                baseColor *= uGrassColor;
            }
        }

        // Apply Breaking Patterns
        if (uIsProxy && uBreakingProgress > 0.0) {
            baseColor = applyBreakingPattern(uBreakingPattern, baseColor, vLocalPos, uBreakingProgress);
        }
    }

    // Apply Lighting
    float sunlight = vLight.x / 15.0;
    float blocklight = vLight.y / 15.0;
    
    // Dynamic Point Light (Held Item)
    float distToLight = distance(fragPos, uPlayerLightPos);
    float dynamicLight = 0.0;
    if (uPlayerLightLevel > 0.0) {
        float attenuation = 1.0 / (1.0 + 0.1 * distToLight + 0.05 * distToLight * distToLight);
        dynamicLight = (uPlayerLightLevel / 15.0) * attenuation;
    }
    
    vec3 lighting = calculateLighting(fragNormal, lightDirection, lightColor * sunlight, ambientLight);
    
    // Add blocklight + dynamic light contribution (warm orange tint)
    float totalBlocklight = max(blocklight, dynamicLight);
    vec3 blocklightCol = vec3(1.0, 0.85, 0.6) * totalBlocklight;
    lighting += blocklightCol;
    
    // Apply Ambient Occlusion
    lighting *= vAO;
    
    fragColor = vec4(lighting * baseColor * brightnessMultiplier, alpha);       

    if (previewPass) {
        fragColor.rgb = mix(fragColor.rgb, vec3(1.0), 0.3);
        fragColor.a *= previewAlpha;
    }
}
