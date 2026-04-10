// Foliage Animation (v1.4)
// Использует 4-ю координату UV (overlayLayer), атрибут веса и внешний множитель swayOverride.

vec3 applyFoliageWind(vec3 worldPos, vec3 localPos, float overlayLayer, float time, bool isProxy, float weight, float swayOverride) {
    // Если оверлея нет, веса нет или анимация принудительно выключена (swayOverride <= 0)
    if (overlayLayer < 0.0 || weight <= 0.0 || swayOverride <= 0.0) return worldPos;

    float swaySpeed = 1.8;
    float swayAmount = 0.08 * swayOverride; // swayOverride теперь множитель интенсивности
    
    // Создаем фазу на основе мировых координат X и Z, чтобы трава на разных блоках качалась асинхронно
    float phase = (worldPos.x * 0.5) + (worldPos.z * 0.5);
    
    // Амплитуда напрямую зависит от веса вершины (0.0 - низ, 1.0 - верх)
    float windX = sin(time * swaySpeed + phase) * swayAmount * weight;
    float windZ = cos(time * swaySpeed * 0.8 + phase) * swayAmount * weight;
    
    return worldPos + vec3(windX, 0.0, windZ);
}
