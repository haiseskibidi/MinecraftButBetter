// Procedural hand conditions (dirt, blood, wetness) for first-person viewmodels

float splatterNoise(vec3 p) {
    float n = hash3D(p);
    float n2 = hash3D(floor(p / 4.0) * 4.0 + 0.5); // Larger blotches
    float n3 = hash3D(floor(p / 12.0) * 12.0 + 0.5); // Large splatters
    return n * 0.15 + n2 * 0.35 + n3 * 0.5;
}

vec3 applyHandConditions(vec3 color, vec3 localPos, vec3 condition, float partWeight) {
    // 1. Честная 3D Пикселизация по сетке вокселей (64x64)
    vec3 gridPos = floor(localPos * 64.0 + 0.0001);
    
    // Сдвиг паттерна для разных костей
    float boneID = floor(partWeight * 4.0 + 0.1); 
    gridPos += vec3(boneID * 31.7, boneID * 13.3, boneID * 23.9); 

    // 2. Блочный шум (Splatter-style)
    float n = splatterNoise(gridPos);

    // 3. Логика распространения (Spread from fingertips/palms)
    // partWeight usually 1.0 at hands, lower at shoulders
    float spreadOffset = (1.0 - partWeight) * 0.7;
    
    // 4. Маска Грязи (Clustered)
    float effectiveDirt = clamp(condition.x * 1.2 - spreadOffset, 0.0, 1.0);
    float dirtThreshold = mix(1.05, 0.35, effectiveDirt); 
    if (n > dirtThreshold) {
        vec3 dColor1 = vec3(0.12, 0.09, 0.07); // Dark mud
        vec3 dColor2 = vec3(0.22, 0.16, 0.11); // Dry dirt
        
        float colorSelect = hash3D(gridPos * 1.13);
        vec3 finalDirtColor = (colorSelect > 0.6) ? dColor2 : dColor1;

        // Darken if wet
        if (condition.z > 0.15) finalDirtColor *= 0.55;
        color = finalDirtColor; 
    }

    // 5. Blood (More vibrant, splattery)
    float nb = splatterNoise(gridPos * 0.87 + vec3(127.0, 42.0, 13.0));
    float effectiveBlood = clamp(condition.y * 1.15 - spreadOffset, 0.0, 1.0);
    if (nb > mix(1.1, 0.45, effectiveBlood)) {
        // More "fleshy" red, slightly glossy
        vec3 bColor = vec3(0.35, 0.01, 0.01);
        if (condition.z > 0.1) bColor *= 0.8; // Diluted blood
        color = bColor;
    }

    // 6. Wetness (Gloss/Darkening)
    if (condition.z > 0.01) {
        color *= (0.7 + (1.0 - partWeight * 0.5 * condition.z) * 0.3);
    }

    return color;
}
