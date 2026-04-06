## Текущий статус проекта "MinecraftButBetter"

## Реализованные фичи
- **Mining Heat VFX (NEW)**:
  - **Tool Incandescence**: Реализован эффект раскаления инструментов и рук при добыче. Жар накапливается пропорционально прогрессу и остывает плавно (2 сек), сохраняясь даже при смене предметов или подборе ресурсов.
  - **Localized Glowing**: Раскаление локализовано: у инструментов краснеет верхняя "рабочая" часть (маска Y), у рук — только костяшки кулака (маска весов костей). Использован насыщенный красный цвет с мягкой пульсацией.
  - **Universal Reset Logic**: `MiningVFXManager` отслеживает хэш предмета и слот. При любой смене или выбрасывании инструмента прогресс на блоке мгновенно обнуляется, предотвращая "читерство".
- **Advanced Data-Driven Crosshairs (v2.0)**:
  - **Animation Scales**: В JSON прицела добавлены параметры `recoilScale` (прыжок при ударе) и `spreadScale` (разлет элементов при прогрессе добычи).
  - **Surgical Rendering**: Прицел больше не меняет цвет/форму сам по себе, вся индикация прогресса перенесена на VFX инструмента. Прицел отрисовывается строго по матрице из JSON через выделенный `crosshairShader`.
- **Viewmodel Rendering Isolation**: Отрисовка рук и предметов вынесена в специализированные шейдеры `viewmodel_vertex/fragment.glsl`, что очистило основной шейдер мира и позволило реализовать точечные эффекты жара.
- **Organic Hand Splatters (v2.0)**:
  - Переработан шейдер `hand_conditions.glsl`. Использование `splatterNoise` позволило заменить ровный шум на реалистичные кляксы и пятна грязи/крови разного размера.
- **Block Placement Animations**: Добавлены 3D-анимации рук при установке блоков и предметов (`block_place`, `item_place`, `hand_place`), улучшающие тактильный отклик.
- **Tools & Balance**: Добавлены деревянная и каменная лопаты. Блок травы (`grass_block`) теперь требует лопату для эффективной добычи.
- **Interaction Fixes**: Устранено дублирование установки блоков при клике на интерактивные объекты (Пни, Костры) через принудительный cooldown после `onUse`.
- **Refactoring (Phase 1)**: Выделен `MiningController` из `InputManager`. Вся логика разрушения блоков, кулдаунов и Weak Spots теперь изолирована, что разгрузило InputManager более чем на 300 строк.
- **Physical Viewmodel System (v5.6)**:
  - **Skeletal Viewmodel Engine**: Переход от простой отрисовки предметов к иерархической скелетной системе (Shoulder -> Forearm -> Hand).
  - **Data-Driven Kinematics**: Реализована детальная кинематика ударов через управление отдельными костями прямо из JSON анимаций.
  - **Fast Interactions System**: Разделены анимации удара (`swing`) и быстрого подбора/сбора (`interact`). Реализованы новые типы анимаций `hand_pickup` и `item_pickup`.
  - **Automatic Orientation & PCA**: Система автоматически выравнивает предметы вертикально на основе анализа текстуры.
- **Advanced Entity Interpolation**: Внедрена поддержка `prevPosition` и `prevRotation` для всех сущностей, обеспечивающая плавность 144Hz+.
- **Dynamic VoxelShape Highlighting**: Обводка блока строится на основе его реального коллайдера (AABB), поддерживая сложные формы (ступени, плиты).
- **Hytale-style Hit-based Block Breaking (v2.1)**:
    - **Weak Spot System**: Реализованы процедурные точки попадания для деревянных блоков.
    - **Progressive Chipping**: Удачные удары оставляют визуальные "шрамы" на поверхности блока.
- **Survivor's Tablet (Journal System)**: Модульная система обучения с современным карточным UI и поддержкой динамических рецептов.
- **Modular Inventory System v4**: Полный переход на Data-Driven GUI, абстракцию хранилищ (`IInventory`) и CSS-подобную систему верстки.
- **Animation Studio (v2.1 Stable)**:
    - **Modular Architecture**: Полное разделение на `State`, `Renderer`, `InputHandler`, `UI` и `Exporter`.
    - **Clean FK Core**: Полностью удалена старая нестабильная реализация IK. Редактор переведен на чистую прямую кинематику (FK) для обеспечения стабильности и предсказуемости анимаций.
    - **Synchronous 3D Gizmos**: Реализованы стрелки перемещения и кольца вращения (Pitch, Yaw, Roll) с поддержкой handle-offset.
    - **Auto-Keying**: Автоматическое создание/обновление ключевых кадров.
    - **Advanced Timeline**: Визуализация ключей, поддержка Shift-привязки и удаления.
    - **Easing & Export**: Поддержка типов сглаживания и экспорт в JSON движка.
    - **Frame-Synced Input**: Исправлена проблема "двойной паузы".
- **Texture Array Graphics Engine (v4.0)**: Полное устранение мерцания текстур через переход на `GL_TEXTURE_2D_ARRAY`.

## В работе
- **Universal IK System (FABRIK)**:
    - Разработка абстрактного ядра `FABRIKSolver` для работы с произвольными цепочками костей.
    - Реализация системы ограничений (Constraints) для суставов.
    - Интеграция IK-таргетов в редактор анимаций.
- **Animation Studio UX**:
    - Реализация Undo/Redo буфера для трансформаций.
    - Копирование и вставка (Copy/Paste) данных ключевых кадров между временными метками.
- **Stamina System**: Implement exhaustion logic for hanging and climbing.
- **Collision Edge Cases**: Refining interactions with corners and slabs.
- **Generic Containers**: Тестирование `ChestScreen` и расширение поддержки сундуков.

## Roadmap
- [x] Milestone 2: Базовые механики выживания (Inventory v3, Hunger, Noise, Parkour v1).
- [ ] Milestone 3: Моддинг-API и Расширяемость (Registry Events, Mod Loader, Physics Registry).
- [ ] Milestone 4: Полноценный геймплей (Мир, Существа, Прогрессия).
