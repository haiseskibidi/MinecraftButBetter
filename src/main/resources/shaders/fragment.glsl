#version 330 core

in vec2 fragTexCoord;
in vec3 fragNormal;
in vec3 fragPos;
in float blockType;
in float neighborData;

out vec4 fragColor;

uniform sampler2D textureSampler;
uniform vec3 lightDirection;
uniform vec3 lightColor;
uniform vec3 ambientLight;
uniform vec4 grassTopUV; // UV координаты grass_block_top.png (min_u, min_v, max_u, max_v)
uniform vec4 leavesUV;   // UV координаты oak_leaves.png (min_u, min_v, max_u, max_v)
uniform vec4 glassUV;    // UV координаты glass.png (min_u, min_v, max_u, max_v)
uniform bool highlightPass; // Если true — рисуем однотонный контур
uniform vec3 highlightColor;
uniform bool previewPass; // Если true — рисуем полупрозрачный блок
uniform float previewAlpha;

void main() {
    if (highlightPass) {
        fragColor = vec4(highlightColor, 1.0);
        return;
    }

    vec4 textureColor = texture(textureSampler, fragTexCoord);

    // Alpha test to discard transparent pixels
    if (textureColor.a < 0.1) discard;

    // Connected Textures for Glass (Type 19)
    if (abs(blockType - 19.0) < 0.1) {
        vec2 localUV = (fragTexCoord - glassUV.xy) / (glassUV.zw - glassUV.xy);
        float t = 0.0625; // Exactly 1 pixel in 16x16 texture
        
        int nMask = int(neighborData + 0.5);
        bool hasLeft  = (nMask & 1) != 0;
        bool hasRight = (nMask & 2) != 0;
        bool hasDown  = (nMask & 4) != 0;
        bool hasUp    = (nMask & 8) != 0;
        
        bool onLeft   = localUV.x < t;
        bool onRight  = localUV.x > (1.0 - t);
        bool onDown   = localUV.y < t;
        bool onUp     = localUV.y > (1.0 - t);

        bool shouldHide = false;
        
        // Horizontal connection logic
        if ((onLeft && hasLeft) || (onRight && hasRight)) {
            // Hide vertical border if it's NOT a rail joint (top/bottom edge of the structure)
            // or if it's an internal corner.
            if (!onDown && !onUp) {
                shouldHide = true;
            } else {
                // It's a corner. Hide ONLY if we have BOTH neighbors (horizontal and vertical).
                // This preserves the outer rails while clearing the internal joints.
                bool hasVerticalNeighbor = (onDown && hasDown) || (onUp && hasUp);
                if (hasVerticalNeighbor) shouldHide = true;
            }
        }
        
        // Vertical connection logic
        if (!shouldHide && ((onDown && hasDown) || (onUp && hasUp))) {
            if (!onLeft && !onRight) {
                shouldHide = true;
            } else {
                // It's a corner. We already checked for both above, but for clarity:
                bool hasHorizontalNeighbor = (onLeft && hasLeft) || (onRight && hasRight);
                if (hasHorizontalNeighbor) shouldHide = true;
            }
        }

        if (shouldHide) {
            // Sample from an empty pixel in the middle
            vec2 sampledLocalUV = vec2(0.5, 0.5);
            vec2 finalUV = glassUV.xy + sampledLocalUV * (glassUV.zw - glassUV.xy);
            textureColor = texture(textureSampler, finalUV);
            if (textureColor.a < 0.1) discard;
        }
    }

    // Grass tinting
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
    
    if (previewPass) {
        fragColor.rgb = mix(fragColor.rgb, vec3(1.0, 1.0, 1.0), 0.3); // Добавляем 30% белого
        fragColor.a *= previewAlpha;
    }
}
