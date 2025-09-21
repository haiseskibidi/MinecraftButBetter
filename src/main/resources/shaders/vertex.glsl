#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoord;
layout(location = 2) in vec3 normal;
layout(location = 3) in float blockTypeAttr;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;

out vec2 fragTexCoord;
out vec3 fragNormal;
out vec3 fragPos;
out float blockType;

void main() {
    fragTexCoord = texCoord;
    fragNormal = normalize(mat3(model) * normal);
    fragPos = vec3(model * vec4(position, 1.0));
    blockType = blockTypeAttr;
    gl_Position = projection * view * model * vec4(position, 1.0);
}
