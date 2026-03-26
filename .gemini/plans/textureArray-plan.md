 1. Рефакторинг ядра графики (engine/graphics)
   * DynamicTextureAtlas -> TextureArray:
       * Заменить glTexImage2D на glTexImage3D (создание массива слоев).
       * Переписать метод build(). Вместо вычисления сетки (grid), теперь каждая текстура — это z-слой.
       * Изменить логику uvFor. Теперь она должна возвращать layerIndex вместо смещения UV.
   * ShaderProgram / Uniforms:
       * Заменить sampler2D на sampler2DArray.
   * Vertex & Mesh:
       * Добавить новое поле в структуру вершины: float layerIndex.
       * Обновить VAO и аттрибуты (теперь в шейдер будет прилетать vec3(u, v, layer)).

  2. Обновление шейдеров (resources/shaders)
   * vertex.glsl:
       * Принимать in float layerIndex.
       * Передавать out vec3 fragTexCoord (вместо vec2).
   * fragment.glsl:
       * Объявить uniform sampler2DArray textureSampler.
       * Использовать texture(textureSampler, fragTexCoord) для семплирования.
       * Удалить хаки с эпсилоном (0.0001) и useMask, так как в массиве текстур края изолированы на уровне железа.

  3. Адаптация Data-Layer (DataLoader, Registry)
   * Обновить загрузку моделей, чтобы они корректно подхватывали индекс слоя из атласа при генерации меша.
   * Проверить, как PrefabManager регистрирует блоки. Убедиться, что Identifier текстуры теперь мапится на int index.

  4. Тонкая настройка качества
   * LOD Bias: Сбросить до 0.0f или сделать адаптивным, чтобы убрать шум при движении.
   * Anisotropic Filtering: Если проект позволяет, включить GL_EXT_texture_filter_anisotropic для еще более четких текстур под углом без мерцания.
