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

    // 1. Базовая яркость (чуть выше, чтобы не было черных зон)
    float ambient = 0.65;
    
    // 2. Основной "Верхний" свет (самый яркий)
    float top = max(dot(fragNormal, TOP_LIGHT_DIR), 0.0) * 0.35;
    
    // 3. Мягкий "Боковой" свет (для объема)
    float side = max(dot(fragNormal, SIDE_LIGHT_DIR), 0.0) * 0.15;
    
    // Итоговое освещение
    float lighting = ambient + top + side;
    
    // Убираем пересвет на боковых гранях
    lighting = clamp(lighting, 0.0, 1.0);

    vec3 color = baseColor * lighting * (brightnessMultiplier * 0.95);
    
    // Небольшое усиление насыщенности (Vibrance)
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(luma), color, 1.1);

    fragColor = vec4(color, textureColor.a);
}
