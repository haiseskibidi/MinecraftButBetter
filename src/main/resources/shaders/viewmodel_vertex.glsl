#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec4 texCoord;
layout(location = 2) in vec3 normal;
layout(location = 3) in float blockTypeAttr;
layout(location = 4) in float neighborDataAttr;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec4 fragTexCoord;
out vec3 fragNormal;
out vec3 fragPos;
out vec3 vLocalPos;
out float blockType;
out float neighborData;

void main() {
    fragTexCoord = texCoord;
    fragNormal = normalize(mat3(model) * normal);
    
    vec4 worldPos = model * vec4(position, 1.0);
    fragPos = worldPos.xyz;
    vLocalPos = position;
    blockType = blockTypeAttr;
    neighborData = neighborDataAttr;

    gl_Position = projection * view * worldPos;
}
