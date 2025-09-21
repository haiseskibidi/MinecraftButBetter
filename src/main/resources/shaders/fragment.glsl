#version 330 core

in vec2 fragTexCoord;
in vec3 fragNormal;
in vec3 fragPos;
in float blockType;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 ambientLight;
uniform vec4 grassTopUV; // UV координаты grass_block_top.png (min_u, min_v, max_u, max_v)
uniform vec4 leavesUV;   // UV координаты oak_leaves.png (min_u, min_v, max_u, max_v)
uniform bool highlightPass; // Если true — рисуем однотонный контур
uniform vec3 highlightColor;

void main() {
    if (highlightPass) {
        fragColor = vec4(highlightColor, 1.0);
        return;
    }

    vec4 textureColor = texture(textureSampler, fragTexCoord);
    
    // Окрашивание только верхней текстуры травы (grass_block_top.png)
    if (abs(blockType - 1.0) < 0.1) { // Если это блок травы
        // Проверяем, находятся ли UV координаты в области grass_block_top.png
        bool isGrassTop = fragTexCoord.x >= grassTopUV.x && fragTexCoord.x <= grassTopUV.z &&
                         fragTexCoord.y >= grassTopUV.y && fragTexCoord.y <= grassTopUV.w;
        
        if (isGrassTop) {
            // Применяем зеленый оттенок к серой верхней текстуре травы
            // Цвет подобран для соответствия Minecraft Plains биому
            vec3 grassTint = vec3(0.486, 0.784, 0.314); // RGB(124, 200, 80) нормализованный
            textureColor.rgb *= grassTint;
        }
    }
    
    // Окрашивание листвы (oak_leaves.png) с alpha-cutout (как в Minecraft)
    if (abs(blockType - 5.0) < 0.1) { // Если это блок листвы (LEAVES = 5)
        bool isLeaves = fragTexCoord.x >= leavesUV.x && fragTexCoord.x <= leavesUV.z &&
                        fragTexCoord.y >= leavesUV.y && fragTexCoord.y <= leavesUV.w;
        if (isLeaves) {
            // Отбрасываем полностью прозрачные пиксели, остальные делаем полностью непрозрачными
            if (textureColor.a < 0.5) discard; 
            vec3 leavesTint = vec3(0.486, 0.784, 0.314);
            textureColor.rgb *= leavesTint;
            textureColor.a = 1.0;
        }
    }
    
    float diffuse = max(dot(fragNormal, -lightDirection), 0.0);
    vec3 lighting = ambientLight + lightColor * diffuse;
    
    fragColor = vec4(lighting * textureColor.rgb, textureColor.a);
}
