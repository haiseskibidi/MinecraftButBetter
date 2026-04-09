#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord;

// Instance data
layout (location = 3) in vec3 instPos;
layout (location = 4) in vec4 instRot; // Quaternion
layout (location = 5) in vec4 instGridInfo; // gx, gy, gz, gridSize
layout (location = 6) in vec2 instTexData; // layerIndex, randomSeed
layout (location = 7) in float instMaterialType;
layout (location = 8) in vec2 instBlockInfo; // type, isTinted

out vec3 fragTexCoord;
out vec3 fragNormal;
out vec3 vLocalNormal;
out vec4 vGridInfo;
out vec2 vTexData;
out float vIsTinted;

uniform mat4 projection;
uniform vec3 lightDirection;
uniform mat4 view;
uniform float uTime;

// Simple hash for randomizing vertex positions
float hash(float n) { return fract(sin(n) * 43758.5453123); }

// Rotate vector by quaternion
vec3 rotate_vector(vec3 v, vec4 q) {
    return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);
}

void main() {
    float gridSize = instGridInfo.w;
    float seed = instTexData.y;
    int mat = int(instMaterialType);
    
    // PROCEDURAL SHAPING
    vec3 localPos = aPos;
    
    // Vertex displacement based on seed and vertex ID
    float vID = float(gl_VertexID);
    vec3 displacement = vec3(
        hash(seed + vID * 1.37) - 0.5,
        hash(seed + vID * 2.11) - 0.5,
        0.0 // Keep it flat for billboarding
    );

    if (mat == 1) { // WOOD (Elongated splinters)
        localPos.y *= 2.5; // Stretch along Y
        localPos.x *= 0.4; // Narrow X
        localPos += displacement * 0.3;
    } else if (mat == 2) { // LEAVES (Small fragments)
        localPos *= 0.8;
        localPos += displacement * 0.4;
    } else { // GENERIC / STONE (Sharp chunks)
        localPos += displacement * 0.6;
    }

    float scale = (1.0 / gridSize);
    
    // BILLBOARDING LOGIC
    // Extract camera basis from view matrix
    vec3 camRight = vec3(view[0][0], view[1][0], view[2][0]);
    vec3 camUp = vec3(view[0][1], view[1][1], view[2][1]);
    
    // Use quaternion rotation for 2D roll (around view axis)
    // We only take the rotation angle from the quaternion for billboards
    float angle = atan(instRot.z, instRot.w) * 2.0; 
    if (mat == 2) angle += sin(uTime * 3.0 + seed * 10.0) * 0.5; // Flutter for leaves
    
    float cosA = cos(angle);
    float sinA = sin(angle);
    
    vec2 rotatedLocal = vec2(
        localPos.x * cosA - localPos.y * sinA,
        localPos.x * sinA + localPos.y * cosA
    );

    vec3 billboardPos = instPos + (camRight * rotatedLocal.x * scale) + (camUp * rotatedLocal.y * scale);
    
    gl_Position = projection * view * vec4(billboardPos, 1.0);
    
    fragTexCoord = vec3(aTexCoord, 0.0); 
    // For billboards, normal is just opposite to camera view
    fragNormal = -vec3(view[0][2], view[1][2], view[2][2]); 
    vLocalNormal = aNormal;
    vGridInfo = instGridInfo;
    vTexData = instTexData;
    vIsTinted = instBlockInfo.y;
}
