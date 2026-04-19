#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 texCoord;
layout(location = 2) in vec3 normal;
layout(location = 3) in float blockTypeAttr;
layout(location = 4) in float neighborDataAttr;
layout(location = 5) in float verticalWeightAttr;
layout(location = 6) in vec2 lightAttr;
layout(location = 7) in float aoAttr;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform bool uIsProxy;
uniform vec3 uWobbleScale;
uniform vec3 uWobbleOffset;
uniform float uWobbleShake;
uniform float uWobbleTime;
uniform float uTime;
uniform float uSwayOverride; // -1.0 = use attribute, 0.0 = force static, 1.0 = force sway
uniform vec3 uOverrideLight; // x=sun, y=block, z=ao. If x >= 0.0, use this instead of attributes.

// Include external modules
#include "include/foliage_animation.glsl"

out vec4 fragTexCoord;
out vec3 fragNormal;
out vec3 fragPos;
out float blockType;
out float neighborData;
out vec3 vLocalPos;
out float vBreakingIntensity;
out vec2 vLight;
out float vAO;
flat out ivec3 vBlockPos;

void main() {
    fragTexCoord = texCoord;

    if (uOverrideLight.x >= 0.0) {
        vLight = uOverrideLight.xy;
        vAO = uOverrideLight.z;
    } else {
        vLight = lightAttr;

        // Unpack block position from aoAttr
        int packedPos = int(aoAttr / 10.0);
        vAO = aoAttr - float(packedPos) * 10.0;
    }

    // Default packedPos for dynamic entities (overridden if attributes used)
    int packedPos = 0;
    if (uOverrideLight.x < 0.0) {
        packedPos = int(aoAttr / 10.0);
    }    
    int localX = packedPos % 16;
    int localZ = (packedPos / 16) % 16;
    int localY = packedPos / 256;
    
    vec3 chunkWorldPos = vec3(model * vec4(0.0, 0.0, 0.0, 1.0));
    vBlockPos = ivec3(round(chunkWorldPos)) + ivec3(localX, localY, localZ);
    
    // Normal Lockdown: Always use original un-animated model matrix for normals to keep lighting stable
    fragNormal = normalize(mat3(model) * normal);
    
    vec3 worldPos = vec3(model * vec4(position, 1.0));
    vBreakingIntensity = 0.0;

    vLocalPos = position;
    if (uIsProxy) {
        vLocalPos.x += 0.5;
        vLocalPos.z += 0.5;
        
        vec3 localDeformed = position; // Start with perfect mathematical alignment
        
        // Only apply deformation math if actually wobbling to prevent floating point drift outlines
        if (uWobbleScale != vec3(1.0) || uWobbleOffset != vec3(0.0) || uWobbleShake > 0.0) {
            vec3 localCenter = vec3(0.0, 0.5, 0.0);
            vec3 dir = position - localCenter;
            localDeformed = localCenter + (dir * uWobbleScale) + uWobbleOffset;
            
            // Hytale-style smooth rotational wobble
            if (uWobbleShake > 0.0) {
                float time = uWobbleTime * 35.0; // Speed of the wobble
                float angleZ = sin(time) * 0.06 * uWobbleShake;
                float angleX = cos(time * 0.8) * 0.06 * uWobbleShake;
                
                mat3 rotZ = mat3(
                    cos(angleZ), -sin(angleZ), 0.0,
                    sin(angleZ),  cos(angleZ), 0.0,
                    0.0,          0.0,         1.0
                );
                mat3 rotX = mat3(
                    1.0, 0.0,         0.0,
                    0.0, cos(angleX), -sin(angleX),
                    0.0, sin(angleX),  cos(angleX)
                );
                
                localDeformed = localCenter + rotZ * rotX * (localDeformed - localCenter);
            }
        }

        worldPos = vec3(model * vec4(localDeformed, 1.0));
        vBreakingIntensity = 1.0;
    }
    
    // Apply wind swaying for grass (texCoord.w is overlayLayer, verticalWeightAttr is weight)
    float swayIntense = (uSwayOverride < 0.0) ? 1.0 : uSwayOverride;
    worldPos = applyFoliageWind(worldPos, position, texCoord.w, uTime, uIsProxy, verticalWeightAttr, swayIntense);

    fragPos = worldPos;
    blockType = blockTypeAttr;
    neighborData = neighborDataAttr;
    gl_Position = projection * view * vec4(worldPos, 1.0);
}
