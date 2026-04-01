#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 texCoord;
layout(location = 2) in vec3 normal;
layout(location = 3) in float blockTypeAttr;
layout(location = 4) in float neighborDataAttr;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

// Impact Wobble Uniforms
uniform bool uIsProxy;
uniform vec3 uHiddenBlockPos;
uniform vec3 uWobbleScale;
uniform vec3 uWobbleOffset;
uniform float uWobbleShake;

out vec3 fragTexCoord;
out vec3 fragNormal;
out vec3 fragPos;
out float blockType;
out float neighborData;
out vec3 vLocalPos;
out float vBreakingIntensity;

void main() {
    fragTexCoord = texCoord;
    fragNormal = normalize(mat3(model) * normal);
    
    vec3 worldPos = vec3(model * vec4(position, 1.0));
    vBreakingIntensity = 0.0;

    if (uIsProxy) {
        vec3 localCenter = vec3(0.0, 0.5, 0.0);
        float jitter = uWobbleShake * 0.015;

        vec3 dir = position - localCenter;
        vec3 localDeformed = localCenter + (dir * uWobbleScale) + uWobbleOffset;
        
        float r1 = fract(sin(dot(position.xyz, vec3(12.9898, 78.233, 45.164))) * 43758.5453);
        localDeformed += (vec3(r1) * 2.0 - 1.0) * jitter;

        worldPos = vec3(model * vec4(localDeformed, 1.0));
        vBreakingIntensity = 1.0;
    } else if (uHiddenBlockPos.y >= 0.0) {
        vec3 blockCenterPos = vec3(model[3][0], model[3][1], model[3][2]);
        if (distance(blockCenterPos, uHiddenBlockPos) < 0.1) {
            worldPos = vec3(9999.0); // Hide original block
        }
    }
    
    fragPos = worldPos;
    blockType = blockTypeAttr;
    neighborData = neighborDataAttr;
    vLocalPos = position;
    gl_Position = projection * view * vec4(worldPos, 1.0);
}
