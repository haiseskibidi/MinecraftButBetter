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
uniform int highlightPass; // 1 = solid color mode, 0 = texture mode
uniform vec3 highlightColor;
uniform bool previewPass; // Если true — рисуем полупрозрачный блок
uniform float previewAlpha;
uniform bool viewModelPass; // Если true — применяем специальное освещение для вида от первого лица

void main() {
    vec4 textureColor = vec4(1.0);
    vec3 baseColor;
    float alpha;

    if (highlightPass != 0) {
        baseColor = highlightColor;
        alpha = 1.0;
    } else {
        textureColor = texture(textureSampler, fragTexCoord);
        if (textureColor.a < 0.1) discard;
        baseColor = textureColor.rgb;
        alpha = textureColor.a;
    }

    // Connected Textures for Glass (Type 19)
    if (highlightPass == 0 && abs(blockType - 19.0) < 0.1) {
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
            if (!onDown && !onUp) {
                shouldHide = true;
            } else {
                bool hasVerticalNeighbor = (onDown && hasDown) || (onUp && hasUp);
                if (hasVerticalNeighbor) shouldHide = true;
            }
        }
        
        // Vertical connection logic
        if (!shouldHide && ((onDown && hasDown) || (onUp && hasUp))) {
            if (!onLeft && !onRight) {
                shouldHide = true;
            } else {
                bool hasHorizontalNeighbor = (onLeft && hasLeft) || (onRight && hasRight);
                if (hasHorizontalNeighbor) shouldHide = true;
            }
        }

        if (shouldHide) {
            vec2 sampledLocalUV = vec2(0.5, 0.5);
            vec2 finalUV = glassUV.xy + sampledLocalUV * (glassUV.zw - glassUV.xy);
            textureColor = texture(textureSampler, finalUV);
            if (textureColor.a < 0.1) discard;
            baseColor = textureColor.rgb;
            alpha = textureColor.a;
        }
    }

    // Grass tinting
    if (highlightPass == 0 && abs(blockType - 1.0) < 0.1) { 
        bool isGrassTop = fragTexCoord.x >= grassTopUV.x && fragTexCoord.x <= grassTopUV.z &&
                         fragTexCoord.y >= grassTopUV.y && fragTexCoord.y <= grassTopUV.w;
        if (isGrassTop) {
            vec3 grassTint = vec3(0.486, 0.784, 0.314); 
            baseColor *= grassTint;
        }
    }
    
    // Окрашивание листвы (oak_leaves.png) с alpha-cutout (как в Minecraft)
    if (highlightPass == 0 && abs(blockType - 5.0) < 0.1) {
        bool isLeaves = fragTexCoord.x >= leavesUV.x && fragTexCoord.x <= leavesUV.z &&
                        fragTexCoord.y >= leavesUV.y && fragTexCoord.y <= leavesUV.w;
        if (isLeaves) {
            if (alpha < 0.5) discard; 
            vec3 leavesTint = vec3(0.486, 0.784, 0.314);
            baseColor *= leavesTint;
            alpha = 1.0;
        }
    }
    
    float diffuse = max(dot(fragNormal, -lightDirection), 0.0);
    vec3 lighting = ambientLight + lightColor * diffuse;
    
    fragColor = vec4(lighting * baseColor, alpha);
    
    if (previewPass) {
        fragColor.rgb = mix(fragColor.rgb, vec3(1.0, 1.0, 1.0), 0.3); 
        fragColor.a *= previewAlpha;
    }
}
