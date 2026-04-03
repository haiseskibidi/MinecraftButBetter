#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 texCoord;
layout(location = 2) in vec3 normal;
layout(location = 3) in float blockTypeAttr;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec3 fragTexCoord;
out vec3 fragNormal;
out vec3 fragPos;
out vec3 vLocalPos;
out float blockType;

void main() {
    fragTexCoord = texCoord;
    fragNormal = normalize(mat3(model) * normal);
    
    vec4 worldPos = model * vec4(position, 1.0);
    fragPos = worldPos.xyz;
    vLocalPos = position;
    blockType = blockTypeAttr;

    gl_Position = projection * view * worldPos;
}
