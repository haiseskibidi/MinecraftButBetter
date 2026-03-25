# План реализации «Реалистичного срубания» (Felling Stages)

## Цель
Реализовать систему постепенного срубания деревьев, при которой натуральное дерево «худеет» с каждым ударом, прежде чем упасть целиком.

## Архитектурные изменения

### 1. Данные (Block Definition & JSON)
- **BlockDefinition.java**: Добавить `int fellingStages` (по умолчанию 0 = мгновенно).
- **DataLoader.java**: Парсить `"fellingStages": N` из JSON.
- **`FellingBlockEntity`**: Создать новый класс для хранения текущей стадии срубания (от `totalStages` до 0).

### 2. Визуальный рендеринг (Shader)
- **fragment.glsl**: Добавить `uniform float uFellingThickness` (0.0 - 1.0).
- **Логика шейдера**:
    - В зависимости от направления (X, Y, Z) делать `discard` пикселей, которые выходят за пределы `uFellingThickness`.
    - Это создаст эффект «подрубания» или «истончения» без растяжения текстуры.

### 3. Механика взаимодействия
- **BlockDefinition.java**: Добавить хук `boolean onBlockBreak(World world, BlockPos pos, Player player)`.
    - Если возвращает `true` -> блок разрушается полностью.
    - Если возвращает `false` -> блок остается (стадия уменьшилась).
- **InputManager.java**: Вызывать `onBlockBreak` вместо `world.destroyBlock` в конце `breakingProgress`.
- **LogBlockDefinition.java**: Реализовать логику стадий:
    - При первом ударе создавать `FellingBlockEntity`.
    - Уменьшать `currentStage`.
    - Если `currentStage == 0` -> вызвать `fellTree`.

### 4. Интеграция
- Переименовать или объединить `CarvingRenderer` в `DynamicBlockRenderer`, чтобы он рисовал как «обтёсанные» пни, так и «худеющие» деревья.

## Этапы реализации
1. Обновление `BlockDefinition` и `DataLoader` (JSON поддержка).
2. Создание `FellingBlockEntity`.
3. Модификация `fragment.glsl` (поддержка `uFellingThickness`).
4. Обновление `InputManager` для поддержки многоступенчатого срубания.
5. Интеграция в `LogBlockDefinition`.

## Верификация
1. Убедиться, что бревно не падает сразу.
2. Проверить, что текстура обрезается (блок становится тоньше), а не сжимается.
3. Проверить, что после N ударов дерево падает целиком.
4. Проверить, что приседание игнорирует стадии.
