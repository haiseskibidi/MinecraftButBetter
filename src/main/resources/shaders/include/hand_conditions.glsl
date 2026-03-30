// Procedural hand conditions (dirt, blood, wetness) for first-person viewmodels

vec3 applyHandConditions(vec3 color, vec3 localPos, vec3 condition, float partWeight) {
    // 1. Честная 3D Пикселизация по сетке вокселей (64x64)
    vec3 gridPos = floor(localPos * 64.0 + 0.0001);
    
    // Сдвиг паттерна для разных костей
    float boneID = floor(partWeight * 4.0 + 0.1); 
    gridPos += vec3(boneID * 31.7, boneID * 13.3, boneID * 23.9); 

    // 2. Блочный шум
    float n = hash3D(gridPos);

    // 3. Логика распространения (Замедлена)
    float spreadOffset = (1.0 - partWeight) * 0.65;
    float effectiveDirt = clamp(condition.x * 1.15 - spreadOffset, 0.0, 1.0);
    
    // 4. Маска Грязи
    float dirtThreshold = mix(1.05, 0.4, effectiveDirt); 
    if (n > dirtThreshold) {
        vec3 dColor1 = vec3(0.12, 0.09, 0.07); 
        vec3 dColor2 = vec3(0.18, 0.14, 0.1);  
        
        float colorSelect = hash3D(gridPos * 1.13);
        vec3 finalDirtColor = (colorSelect > 0.7) ? dColor2 : dColor1;

        if (condition.z > 0.15) finalDirtColor *= 0.6;
        color = finalDirtColor; 
    }

    // 5. Blood (Тоже мелкая и пиксельная)
    float nb = hash3D(gridPos * 0.91 + vec3(42.0));
    float effectiveBlood = clamp(condition.y * 1.1 - spreadOffset, 0.0, 1.0);
    if (nb > mix(1.1, 0.5, effectiveBlood)) {
        color = vec3(0.22, 0.01, 0.01);
    }

    // 6. Wetness
    if (condition.z > 0.01) {
        color *= (0.75 + (1.0 - partWeight * 0.4 * condition.z) * 0.25);
    }

    return color;
}
