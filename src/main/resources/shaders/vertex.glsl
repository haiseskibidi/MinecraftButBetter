#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 texCoord;
layout(location = 2) in vec3 normal;
layout(location = 3) in float blockTypeAttr;
layout(location = 4) in float neighborDataAttr;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

uniform bool uIsProxy;
uniform vec3 uHiddenBlockPos;
uniform vec3 uWobbleScale;
uniform vec3 uWobbleOffset;
uniform float uWobbleShake;
uniform float uWobbleTime;

out vec4 fragTexCoord;
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

        vec3 dir = position - localCenter;
        vec3 localDeformed = localCenter + (dir * uWobbleScale) + uWobbleOffset;
        
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

        worldPos = vec3(model * vec4(localDeformed, 1.0));
        vBreakingIntensity = 1.0;
    }
    
    fragPos = worldPos;
    blockType = blockTypeAttr;
    neighborData = neighborDataAttr;
    vLocalPos = position;
    gl_Position = projection * view * vec4(worldPos, 1.0);
}
