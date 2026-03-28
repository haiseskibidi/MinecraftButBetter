#version 330 core

in vec3 fragTexCoord;
in vec3 fragNormal;
in float blockType;

out vec4 fragColor;

uniform sampler2DArray textureSampler;
uniform float brightnessMultiplier = 1.0;
uniform vec3 tintColor = vec3(0.486, 0.784, 0.314); 

// Константы для освещения
const vec3 TOP_LIGHT_DIR = vec3(0.0, 1.0, 0.0);
const vec3 SIDE_LIGHT_DIR = normalize(vec3(0.5, 0.2, 0.5)); // Мягкий свет спереди-справа

void main() {
    vec4 textureColor = texture(textureSampler, fragTexCoord);
    if (textureColor.a < 0.5) discard;
    
    vec3 baseColor = textureColor.rgb;

    if (blockType < -0.5) {
        baseColor *= tintColor;
    }

    // 1. Базовая яркость (теплый ambient)
    float ambient = 0.6; 
    
    // 2. Основной "Верхний" свет
    float top = max(dot(fragNormal, TOP_LIGHT_DIR), 0.0);
    // Мягкое квантование
    top = smoothstep(0.2, 0.8, top) * 0.45;
    
    // 3. Мягкий "Боковой" свет
    float side = max(dot(fragNormal, SIDE_LIGHT_DIR), 0.0);
    side = smoothstep(0.1, 0.9, side) * 0.25;
    
    // Итоговое освещение
    float lighting = ambient + top + side;
    
    // Убираем пересвет
    lighting = clamp(lighting, 0.0, 1.0);

    vec3 color = baseColor * lighting * (brightnessMultiplier * 1.05);
    
    // Усиление насыщенности (Vibrance)
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(luma), color, 1.2);

    fragColor = vec4(color, textureColor.a);
}
