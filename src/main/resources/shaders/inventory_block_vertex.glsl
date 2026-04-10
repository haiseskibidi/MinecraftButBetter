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
out float blockType;
out float neighborData;

void main() {
    fragTexCoord = texCoord;
    
    // Вычисляем матрицу нормалей для корректной обработки отрицательного масштаба (Y-flip)
    mat3 normalMatrix = transpose(inverse(mat3(model)));
    fragNormal = normalize(normalMatrix * normal);
    
    blockType = blockTypeAttr;
    neighborData = neighborDataAttr;
    gl_Position = projection * view * model * vec4(position, 1.0);
}
