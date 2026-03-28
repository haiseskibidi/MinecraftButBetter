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
out float blockType;

void main() {
    fragTexCoord = texCoord;
    
    // Вычисляем матрицу нормалей для корректной обработки отрицательного масштаба (Y-flip)
    mat3 normalMatrix = transpose(inverse(mat3(model)));
    fragNormal = normalize(normalMatrix * normal);
    
    blockType = blockTypeAttr;
    gl_Position = projection * view * model * vec4(position, 1.0);
}
