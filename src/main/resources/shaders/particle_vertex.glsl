#version 330 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 aTexCoord;

// Instance data
layout (location = 2) in vec4 instPosRoll; // x, y, z, roll
layout (location = 3) in vec2 instVisual;  // scale, alpha
layout (location = 4) in vec3 instTexData; // layer, snippetX, snippetY
layout (location = 5) in vec3 instColor;

out vec3 fragTexCoord;
out float fragAlpha;
out vec3 vColor;
out vec3 vNormal;

uniform mat4 projection;
uniform mat4 view;

void main() {
    vec3 worldPos = instPosRoll.xyz;
    float roll = instPosRoll.w;
    float scale = instVisual.x;
    
    // BILLBOARDING: Extract camera right and up vectors from view matrix
    vec3 camRight = vec3(view[0][0], view[1][0], view[2][0]);
    vec3 camUp = vec3(view[0][1], view[1][1], view[2][1]);
    
    // Apply 2D Roll (rotation around camera-facing axis)
    float cosR = cos(roll);
    float sinR = sin(roll);
    
    vec2 rotatedPos = vec2(
        aPos.x * cosR - aPos.y * sinR,
        aPos.x * sinR + aPos.y * cosR
    );
    
    vec3 vertexWorldPos = worldPos + (camRight * rotatedPos.x * scale) + (camUp * rotatedPos.y * scale);
    
    gl_Position = projection * view * vec4(vertexWorldPos, 1.0);
    
    // Pass to fragment
    fragTexCoord = vec3(aTexCoord * 0.25 + instTexData.yz, instTexData.x);
    fragAlpha = instVisual.y;
    vColor = instColor;
    
    // Normal for billboards is always facing camera
    vNormal = -vec3(view[0][2], view[1][2], view[2][2]);
}
