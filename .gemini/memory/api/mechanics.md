# Zenith API: World, Physics & Mechanics

Этот документ содержит полные технические спецификации физики мира, взаимодействия с блоками, локомоции и индустриальных систем.

---

## 1. Блоки и Взаимодействие (JSON)

### 1.1. Базовые свойства
**Пример (`grass_block.json`):**
```json
{
    "identifier": "zenith:grass_block",
    "translationKey": "block.zenith.grass_block",
    "hardness": 0.6,
    "textures": { "top": "grass_block_top.png", "bottom": "dirt.png", "side": "grass_block_side.png" },
    "requiredTool": "shovel",
    "tags": ["zenith:tinted"]
}
```

### 1.2. Weak Spots и Progressive Drops
**Пример (`rusty_car_chassis.json`):**
```json
{
  "identifier": "zenith:rusty_car_chassis",
  "hardness": 6.0,
  "mining_logic": {
    "strategy": "weak_spots",
    "precision": 0.18,
    "miss_multiplier": 0.2
  },
  "drops": [{ "item": "zenith:scrap_metal", "chance": 0.25, "drop_on_hit": true }]
}
```

---

## 2. Физика Сущностей (Entity System)
... (существующий текст) ...

---

## 3. Actions (Действия)
Все затраты выносливости и шум вынесены в `zenith/actions/*.json`.
**Пример (`walk.json`):**
```json
{
  "staminaCostPerSecond": 0.5,
  "hungerCostPerSecond": 0.05,
  "noiseLevel": 0.3
}
```

---

## 4. Установка блоков и Foraging
- **Превью установки:** Голограмма (Preview) отображается **только когда игрок крадется** (Sneaking).
- **3D Foraging:** Мелкие ресурсы (палки, камни) — это сущности `ResourceEntity`. Собираются мгновенным кликом без кулдауна.
- **144Hz+ Interpolation**: Разделение физического тика (20Hz) и рендеринга. Использует `prevPosition`, `prevRotation` и коэффициент `alpha`.
- **AABB Collision**: Скользящая коллизия с epsilon-отступами.
- **Unstuck**: Поэтапное выталкивание вверх при застревании в блоках.
- **Ground Locking**: Обнуление вертикальной скорости на земле для предотвращения микро-колебаний.

---

## 3. Паркур и Локомоция
- **Bezier 3D Path**: Подъем на уступы по квадратичной кривой Безье.
- **Ledge Detection**: Многоступенчатый рейкаст для поиска краев.
- **Ledge Grab Snapping**: Автоматическое выравнивание игрока параллельно стене при зацепе.
- **Action System**: Все затраты (стамина, шум, голод) описаны в `zenith/actions/*.json`. Срыв с уступа при 0 стамины.

---

## 4. Рецепты и In-World Crafting
Реализуется через интерфейс `ICraftingSurface` (например, пень `StumpBlockEntity`).

**Пример 1: Napping (Обтёсывание по паттерну)**
```json
{
  "identifier": "zenith:unfired_vessel",
  "type": "napping",
  "input": "zenith:clay_ball",
  "result": { "item": "zenith:unfired_vessel", "count": 1 },
  "pattern": [
    " ### ",
    "#   #",
    "#   #",
    "#   #",
    " ### "
  ]
}
```

**Пример 2: Carving (Вырубание)**
```json
{
  "identifier": "zenith:oak_log_carving",
  "type": "carving",
  "input": "zenith:oak_log",
  "tool": "zenith:stone_knife",
  "intermediate": "zenith:unfinished_stump",
  "result": "zenith:stump"
}
```

- **CraftingLayoutEngine**: Автоматическое масштабирование предметов на поверхности блока (сетка 3x3).

---

## 5. Treecapitator (Рубка деревьев)
- **BFS Алгоритм**: Поиск живого дерева. Отличает постройки игрока по флагу **BIT_NATURAL (0x80)** в метаданных.
- **Dynamic Stages**: Количество стадий срубания зависит от высоты дерева.
- **Drop Resolution**: Обратная конвертация технических блоков в оригинальные бревна при падении.

---

## 6. Индустриальная система (Tier 1)
- **IEnergyStorage**: Интерфейс для блоков с энергией.
- **ITickable**: Логика передачи энергии каждый тик.
- **Cable Logic**: Принцип "сообщающихся сосудов" — выравнивание уровня энергии между соседними узлами.
- **Global Noise**: Агрегация шума от работающей техники. Моби мобам отвлекаться на звук.

---

## 7. Система Зон Взаимодействия (Interaction Zones)
Для предотвращения «срабатывания» блока при наведении на его декоративные части используется точечная интерактивность.

- **InteractionZone**: Класс-контейнер для `AABB` (в локальных координатах 0-1) и типа действия.
- **isInteractableAt**: Метод `BlockDefinition`, проверяющий, попал ли луч в одну из зон.
- **Стандарт реализации**: 
  - Если блок функционален только частью (например, рабочая поверхность пня), переопределяйте `getInteractionZones`.
  - Это автоматически синхронизирует прицел, HUD-подсказки и клики игрока.

---

## 8. Прочие Data-Driven Механики

### 8.1. Декоративные Сущности (Entity Registry)
Вместо создания тяжелых блоков для мелких декоративных объектов (например, лежащие бревна, палки), Zenith использует легковесные сущности из реестра `EntityRegistry`.
Конфигурация (`zenith/entities/log_pile.json`):
```json
{
    "identifier": "zenith:log_pile",
    "modelType": "block",
    "texture": "zenith:oak_log",
    "visualScale": [0.55, 0.55, 0.95],
    "hitbox": [0.55, 0.55, 0.95]
}
```

### 8.2. Data-Driven Структуры (Structures)
Генерация построек и зданий описана в `src/main/resources/zenith/structures/`.
Используется матричная система по слоям (`layers`) с привязкой символов к блокам через `palette`.
Пример палитры:
```json
"palette": {
  "C": "zenith:cyan_concrete",
  ".": "zenith:air",
  " ": "-1" // Пропустить (не заменять существующий блок)
}
```

---

## 9. Глобальные Настройки и Реестры (Registry)

Для тонкой настройки движка без перекомпиляции используются глобальные конфигурации в папке `src/main/resources/zenith/registry/`:

### 9.1. Физика Игрока (`physics.json`)
Хранит все ключевые константы локомоции: хитбоксы (`standingHeight`, `sneakingHeight`), дистанцию взаимодействия (`grabDistance`), высоту паркура (`maxGrabHeight`), а также множители скорости и базовый кулдаун добычи (`baseMiningCooldown`). Изменение этого файла меняет физику налету.

### 9.2. Спавн ресурсов на земле (`scavenge.json`)
Конфигурирует вероятность появления ресурсов (сущностей из `EntityRegistry`) на поверхности блоков при генерации мира:
```json
[
  { "block": "zenith:stick", "chance": 0.015 },
  { "block": "zenith:rock", "chance": 0.01 }
]
```

### 9.3. Типы древесины (`wood_types.json`)
Массив идентификаторов базовых бревен. Используется в системе Treecapitator для маппинга индекса из метаданных (0-5) в конкретный блок при выпадении лута.
