# Code Map: Zenith

## Asset Map
### src/main/resources/zenith/registry
Назначение: Глобальные конфигурации и реестры данных.
- `physics.json`: Физические параметры мира.
- `world.json`: Настройки времени, скорости цикла дня/ночи и цветов освещения (Sun/Moon/Ambient).
- `celestial.json`: Визуальные параметры небесных тел (текстуры, масштаб, процедурные пиксельные сетки).
- `wood_types.json`: Список пород деревьев.

### src/main/resources/zenith/textures
Назначение: Полный набор оригинальных ассетов zenith.
- **block/**: Текстуры блоков (16x16).
- **item/**: Иконки предметов.
- **gui/**: Элементы интерфейса.
    - `crosshair.png`: Прицел.
- **entity/**: Текстуры сущностей.

### src/main/resources/zenith/gui
Назначение: Конфигурации игровых интерфейсов в формате JSON.
- `player_inventory.json`: Главное окно игрока.
- `hotbar.json`: Конфигурация HUD-хотбара.
- `chest.json`: Интерфейс сундуков.

### src/main/java/com/za/zenith/engine/graphics/model (UPDATED)
- **ModelNode.java**: Узел скелета. Теперь имеет два состояния вращения: `animRotation` (Euler, v1) и `animRotationQuat` (Quaternion, v2).
- **ViewmodelController.java**: Парсер треков. Отвечает за применение AnimationProfile. Внедрена система `Snapshot Buffer` для хранения прошлых поз и плавной `slerp`-интерполяции (Cross-fade) при смене анимаций или предметов.
- **HeldItemRenderer.java**: Отрисовывает предметы в руках. Очищен от хардкода.
- **ViewmodelRenderer.java**: Центрирует блоки математически точно в сокете (socket_palm), опираясь на параметры `ViewmodelComponent`.
- **GripRegistry.java** / **GripDefinition.java**: Система Data-Driven пресетов для костей пальцев (например, `flat_sheet` для удержания блоков снизу).

## Voxel Lighting & Celestial Systems (v1.0 NEW)
### com.za.zenith.world.lighting
- **LightEngine.java**: Ядро системы освещения. Реализует BFS-распространение блочного света (от источников) и первичную заливку солнечного света (`generateInitialSunlight`) сверху вниз.
- **WorldSettings.java**: Data-Driven контейнер для настроек времени и цветов освещения (загружается из `world.json`).

### com.za.zenith.engine.graphics
- **SkyRenderer.java**: Специализированный рендерер небесных тел. Отрисовывает Солнце и Луну как билборды в пространстве вида (View Space) с поддержкой процедурных кругов и пиксельных сеток.
- **SkySettings.java**: Конфигурация параметров неба (загружается из `celestial.json`).
- **Renderer.java (UPDATED)**: Управляет динамической сменой освещения. Смешивает цвета `sunLightColor` и `moonLightColor` на основе `worldTime`, инвертирует направление теней ночью.

### com.za.zenith.world.chunks
- **Chunk.java (UPDATED)**: Добавлен массив `byte[] lightData` для компактного хранения Sunlight и Blocklight (по 4 бита).
- **ChunkMeshGenerator.java (UPDATED)**: 
    - **Smooth Lighting**: Расчет среднего освещения 4-х вокселей для каждой вершины.
    - **Ambient Occlusion**: Затенение углов на основе анализа соседних блоков.

### src/main/resources/shaders (UPDATED)
- **sky_vertex/fragment.glsl**: Шейдеры для отрисовки неба. Поддерживают процедурный "Glow" эффект и переключение между текстурой и пиксельной сеткой.
- **vertex/fragment.glsl**: Обновлены для приема атрибутов `vLight` и `vAO` и финального расчета освещенности.

## UI & Animation Editor (v1.1 MODULAR)
### com.za.zenith.engine.graphics.ui.editor.animation
- **AnimationEditorScreen.java**: Главный оркестратор. Делегирует задачи специализированным модулям.
- **AnimationEditorState.java**: Модель данных. Хранит ключи, треки, текущее время и выбранные объекты.
- **AnimationEditorRenderer.java**: Рендеринг 3D сцены студии, отрисовка Гизмо и "призраков" кадров. Очищен от логики IK.
- **EditorInputHandler.java**: Логика взаимодействия: Ray-OBB Picking, Grab/Rotate манипуляции через чистый FK, горячие клавиши.
- **EditorUI.java**: Отрисовка всех панелей интерфейса (Parts, Properties, Timeline). Удален статус IK и управление таргетами.
- **AnimationExporter.java**: Конвертация состояния в JSON формат движка.
- **TransformController.java**: Низкоуровневая логика трансформаций. Реализует перемещение и вращение костей в 3D пространстве с учетом иерархии.

### com.za.zenith.engine.graphics.ui.InventoryScreen (UPDATED)
Назначение: Базовый класс для всех экранов с инвентарями.
Функции: Реализует метод `handleQuickMove` — универсальную Data-Driven логику для Shift+Click, использующую правила из JSON. Содержит абстрактный метод `getScreenIdentifier()`.

### com.za.zenith.engine.graphics.ui.ChestScreen (UPDATED)
Назначение: Универсальный экран для любых контейнеров.
Функции: Принимает динамический `Identifier guiId` в конструкторе. Загружает разметку из соответствующего JSON файла через `InventoryLayout`. Поддерживает отображение нескольких инвентарей (контейнер + игрок).

### com.za.zenith.engine.graphics.ui.InventoryLayout (UPDATED)
Назначение: Движок динамической разметки интерфейсов.
Функции: Генерирует позиции слотов и групп на основе JSON. Поддерживает `Map<String, IInventory>` для работы с несколькими источниками данных одновременно. Реализует логику `relativeTo` (относительное позиционирование групп).

### com.za.zenith.engine.graphics.ui.SlotUI (UPDATED)
Назначение: Визуальное представление слота.
Функции: Содержит `groupId` для связи слота с правилами быстрого перемещения в JSON конфигурации.

### com.za.zenith.engine.graphics.ui.GUIConfig (UPDATED)
Назначение: Модель данных для JSON-конфигураций интерфейса.
Функции: Расширена полями `inventorySource`, `startIndex`, `slotsCount` и `quickMoveTo` для каждой группы.

### com.za.zenith.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Модульный рендерер интерфейса (Facade).
Функции: Является точкой входа для рендеринга всего UI, но делегирует работу специализированным суб-рендерерам: `UIPrimitives` (базовые формы), `SlotRenderer` (предметы/слоты), `HUDRenderer` (игровой оверлей, стамина, голод), `InventoryScreenRenderer` (инвентарь и dev-панель) и `MenuRenderer`.

## World & Items (UPDATED)
### com.za.zenith.world.World (UPDATED)
Назначение: Управление состоянием мира, сущностями и чанками.
Функции: Хранит `blockDamageMap` с объектами `BlockDamageInstance`. Реализует `notifyNeighbors(BlockPos)` для запуска обновлений выживания. Метод `destroyBlock(pos, player)` теперь является единой точкой входа для разрушения блоков с выпадением лута и частицами.

### com.za.zenith.world.blocks.BlockDefinition (UPDATED)
Назначение: Физические и визуальные свойства блока.
Функции: Хранит `requiresSupport`. Реализует `onNeighborChange` для логики выживания и цепочек `DOUBLE_PLANT`. Метод `spawnDrops` инкапсулирует логику выпадения предметов (включая `DropRule`).

### com.za.zenith.engine.input.MiningController (UPDATED)
Назначение: Контроллер процесса добычи блоков.
Функции: Синхронизирует прогресс разрушения. Теперь полностью очищен от ручной логики дропа и обработки двойных растений, делегируя это `World.destroyBlock`.

### com.za.zenith.world.items.Item (UPDATED)
Назначение: Базовый класс предмета.
Функции: Управление компонентами и индивидуальным `interaction_cooldown`.

### com.za.zenith.world.items (UPDATED)
- **ItemSearchEngine.java**: Универсальный движок фильтрации. Поддерживает поиск по локализованным именам, ID и путям.
- **Item.java**: Базовый класс предмета. Теперь содержит `Gender`, `Tags` и `defaultRarity`.
- **ItemStack.java**: Состояние предмета в инвентаре. Хранит `rarity`, `activeAffixes` и `StatContainer`. Поддерживает генерацию гендерно-зависимых имен и авто-перенос в тултипах.
- **ItemRegistry.java**: Центральный реестр предметов.
- **DataLoader.java**: Загрузка JSON. Обновлен для парсинга RPG-полей предметов (rarity, gender, tags) и Loot Tables.

### com.za.zenith.world.items.stats (NEW)
- **StatRegistry.java**: Реестр определений всех игровых характеристик.
- **RarityRegistry.java**: Реестр уровней редкости (Common...Legendary).
- **AffixRegistry.java**: Реестр префиксов/суффиксов.
- **StatContainer.java**: Хранилище и калькулятор модификаторов для Player и ItemStack. Поддерживает динамический пересчет при добавлении аффиксов.
- **RarityDefinition.java**: Модель данных редкости, включая `colorCode` для UI.
- **AffixDefinition.java**: Модель данных аффикса с условиями применимости (`applicableTo`).

### com.za.zenith.world.items.loot (NEW)
- **LootTable.java**: Определение пулов и весов выпадения предметов.
- **LootGenerator.java**: Ядро рандомизации. Реализует ролл редкости, аффиксов и предметов из пулов с учетом весов контейнера.

- `com.za.zenith.engine.graphics.ui`
    - `UISearchBar.java`: Переиспользуемый компонент поисковой строки.
    - `FontRenderer.java`: Отрисовка текста. Реализована поддержка цветовых кодов и стилей через символ `$`.
    - `InventoryScreen.java`: Базовый класс экранов инвентаря.
    - `GUIConfig.java`: Модель конфигурации GUI. Добавлена поддержка `HUDElementConfig` для гибкой настройки HUD.
- `com.za.zenith.engine.graphics.ui.renderers`
    - `HUDRenderer.java`: Отрисовка HUD. Реализована полная поддержка `hud.json` и динамическое масштабирование имен предметов.
    - `InventoryScreenRenderer.java`: Отрисовка инвентарей. Исправлено цветовое кодирование отрицательных статов в тултипах.

## Entity System (v5.6 NEW)
### com.za.zenith.entities.Entity (UPDATED)
Назначение: Базовый класс для всех сущностей (игрок, мобы, предметы).
Функции: Реализует интерполяцию (`prevPosition`, `prevRotation`), систему выталкивания из блоков (`Unstuck`) и общую логику перемещения `move()` с коллизиями.

### com.za.zenith.entities.ItemEntity (UPDATED)
Назначение: Сущность предмета, выброшенного в мир.
Функции: Реализует `Ground Lock` (отключение гравитации на земле), стабилизацию вращения (выравнивание плашмя при приземлении) и кастомную гравитацию на основе веса.

### com.za.zenith.entities.DecorationEntity (UPDATED)
Назначение: Универсальная декоративная сущность.
Функции: Физика и коллизии полностью отключены для предотвращения нежелательных перемещений при изменении ландшафта (например, в ямах для обжига). Поддерживает вращение и кастомный масштаб из JSON.

### com.za.zenith.entities.ResourceEntity (NEW)

### com.za.zenith.engine.graphics.ui.FontRenderer (UPDATED)
Назначение: Продвинутая отрисовка текста с поддержкой эффектов.
Функции: Поддерживает цветовые коды `$0-f` и динамические анимации: `$z` (Rainbow), `$g` (Glow), `$v` (Wave), `$q` (Shake). Реализует метод `wrapText` для корректного переноса длинных строк.

### com.za.zenith.world.items.Item (UPDATED)
Назначение: Базовый класс предмета.
Функции: Расширен полем `descriptionKey` для Markdown-описаний. Исправлена логика `getMiningSpeed`: инструменты с флагом `isEffectiveAgainstAll` теперь применяют полную эффективность даже к блокам без типа (стекло, бетон).

### com.za.zenith.world.items.ItemStack (UPDATED)
Назначение: Состояние предмета.
Функции: Добавлена защита от NPE (null-check) в конструкторе. Реализована поддержка бесконечной прочности (флаг `-1`).

### src/main/resources/shaders/include/post_stack.glsl (NEW)
Назначение: Унифицированный модуль AAA-постпроцессинга.
Функции: Применяет Stylized Crease AO, Atmospheric Fog, Filmic Contrast, Balanced Vibrance и Cinematic Vignette. Вызывается из `fxaa_fragment.glsl` и `passthrough_fragment.glsl` до сглаживания.

### com.za.zenith.engine.graphics.model.ViewmodelPhysics (UPDATED)
Назначение: Физический симулятор рук.
Функции: Внедрен зажим `deltaTime` (max 0.05s) и проверки `Float.isFinite()`, предотвращающие исчезновение рук при лагах. Иерархия переведена на Semi-implicit Euler интеграцию для стабильности.

### com.za.zenith.engine.graphics.Shader (UPDATED)
Назначение: Обертка над шейдерной программой.
Функции: Добавлен метод `setVector4f()` для передачи координат и интенсивности шрамов.

### com.za.zenith.world.World (UPDATED)
Назначение: Управление состоянием мира, сущностями и чанками.
Функции: Хранит `blockDamageMap` с объектами `BlockDamageInstance` (урон + история шрамов в `Vector4f`). Реализует логику задержки регенерации (5 сек) и плавного затухания шрамов (изменение компоненты `w`).

### com.za.zenith.engine.graphics.Renderer (UPDATED)
Назначение: Главный контроллер рендеринга.
Функции: Управляет проходами отрисовки. Реализован метод `renderPersistentScars` для отображения повреждений всех блоков мира с использованием `glPolygonOffset`. Исправлено выравнивание меша `ItemEntity`.

### com.za.zenith.engine.graphics.vfx.MiningVFXManager (NEW)
Назначение: Менеджер визуальных эффектов добычи.
Функции: Рассчитывает уровень раскаления (`heatLevel`) инструментов и рук. Обеспечивает плавное остывание и универсальный сброс прогресса добычи при смене/выбрасывании предмета (через отслеживание `identityHashCode` и слота).

### src/main/resources/shaders (NEW SHADERS)
- **viewmodel_vertex/fragment.glsl**: Изолированные шейдеры для рук и предметов. Поддерживают процедурное раскаление (`uMiningHeat`), маскировку по весам костей и поддержку биомного тинта для блоков в руках.
- **crosshair_vertex/fragment.glsl**: Шейдеры динамического прицела. Поддерживают data-driven анимации разлета (`spreadScale`) и отдачи (`recoilScale`).

### com.za.zenith.engine.graphics.ui.crosshair (UPDATED)
- **CrosshairDefinition**: Добавлены поля `recoilScale` и `spreadScale` для настройки анимаций в JSON.
- **CrosshairManager**: Управляет состояниями. Приоритет `MINING` восстановлен для корректного отображения на интерактивных блоках (пни).
- **CrosshairRenderer**: Теперь строго использует специализированный шейдер для отрисовки матриц из JSON. Реализует анимацию разлета элементов.

### com.za.zenith.engine.graphics.model (UPDATED)
- **ViewmodelRenderer**: Теперь принимает `heat` и распределяет его между руками и инструментами. Доступен геттер для `heldItemRenderer`.
- **HeldItemRenderer**: Реализует точечную передачу уровня жара инструменту. Метод `getOrGenerateMesh` сделан публичным для системы выбора.

### com.za.zenith.world.blocks (UPDATED)
- **CarTireBlockDefinition.java**: Базовый декоративный блок покрышки. Поддерживает трансформацию в `TireWithBoard` при Shift+ПКМ досками.
- **TireWithBoardBlockDefinition.java**: Промежуточная стадия сборки стола. Трансформируется в `ScavengerTable` при Shift+ПКМ листом металла.
- **ScavengerTableBlockDefinition.java**: Блок Стола Мусорщика. Делегирует взаимодействие `ScavengerTableBlockEntity`.
- **StumpBlockDefinition.java**: Определение блока для пня (Stump).

### com.za.zenith.world.blocks.entity (UPDATED)
- **ScavengerTableBlockEntity.java**: Сущность Стола Мусорщика. Реализует `ICraftingSurface`. Хранит 9 слотов инвентаря и прогресс крафта.
- **StumpBlockEntity.java**: Сущность блока для пня (Stump).

## Animation & Locomotion System (v4.5 UPDATED)
### com.za.zenith.entities.Player (UPDATED)
Назначение: Главная сущность игрока.
Функции: Управление инвентарем, статами (голод, стамина), паркуром и анимациями. Поддерживает методы `swing()` (удар) и `interact()` (быстрый сбор/подбор). Поддерживает методы `swing()` (удар) и `interact()` (быстрый сбор/подбор).

### com.za.zenith.engine.input.MiningController (NEW)
Назначение: Контроллер процесса добычи блоков.
Функции: Управляет таймерами (cooldown, breakingDelay), генерирует Weak Spots, рассчитывает прогресс разрушения. Автоматически выбирает тип анимации (`swing` vs `interact`) на основе прочности блока. Передает данные для отрисовки прокси-блока в `Renderer`.

### com.za.zenith.engine.core.GameLoop (UPDATED)
Назначение: Главный цикл. Добавлена поддержка переключения в режим Студии (F8) и изоляция обновлений.

## Physical Viewmodel System (v5.0 NEW)
### com.za.zenith.engine.graphics.Mesh (UPDATED)
Назначение: Низкоуровневая обертка над VBO/VAO.
Функции: Отрисовка, очистка ресурсов. Добавлены методы `getMin()` и `getMax()` для вычисления Bounding Box меша в реальном времени.

### com.za.zenith.engine.graphics.model.ViewmodelRenderer (UPDATED)
Назначение: Рендерер рук игрока.
Функции: Рендерит иерархия костей. Делегирует отрисовку удерживаемых предметов классу `HeldItemRenderer`.

### com.za.zenith.engine.graphics.model.HeldItemRenderer (NEW)
Назначение: Специализированный рендерер для предметов и блоков в руках.
Функции: Управляет трансформациями (смещение, поворот, масштаб) относительно костей кисти. Реализует динамическое прижатие блоков к ладони на основе их геометрии.

### com.za.zenith.world.items.ItemMeshGenerator (UPDATED)
Назначение: Генератор 3D мешей из 2D текстур.
Функции: Использует PCA (Principal Component Analysis) для автоматического определения ориентации предмета и точки хвата. Генерирует вертикально выровненные меши.

### com.za.zenith.engine.graphics.model.ViewmodelPhysics
Назначение: Физический симулятор для рук и предметов.
Функции: Решает дифференциальные уравнения 2-го порядка (пружина-масса-демпфер) для расчета инерции и веса.

### com.za.zenith.engine.graphics.model.ViewmodelController
Назначение: Менеджер скелетных анимаций.
Функции: Применяет AnimationProfile к костям скелета (поддержка парсинга треков `nodeName:track`), поддерживает наслоение (blending) нескольких анимаций и динамическое масштабирование времени (`baseMiningCooldown`).

### com.za.zenith.engine.graphics.model.ViewmodelMeshGenerator
Назначение: Генератор воксельных мешей для костей.
Функции: Создает оптимизированную геометрию на основе кубов, описанных в JSON.

### com.za.zenith.engine.graphics.model.ik (NEW)
- **FABRIKSolver.java**: Математическое ядро IK. Реализует алгоритм FABRIK с поддержкой Pole Targets для контроля сгибов.
- **IKChain.java**: Контейнер для цепочки костей, хранит целевые позиции и ограничения.
- **constraints/IKConstraint.java**: Базовый интерфейс для ограничений суставов.
- **constraints/HingeConstraint.java**: Ограничение вращения по одной оси (для локтей и коленей).

### com.za.zenith.engine.graphics.ui.editor.animation (UPDATED)
- **EditorIKManager.java**: Управляет несколькими IK-цепями одновременно. Поддерживает `autoSetup` (руки/ноги) и запекание поз в ключи.
- **AnimationEditorRenderer.java**: Визуализирует цели IK (желтые) и Pole Targets (синие).
- **TransformController.java**: Теперь поддерживает прямой ввод в IK-таргеты при перемещении эффекторов (кистей/стоп).

### com.za.zenith.engine.graphics.model.ModelRegistry
Назначение: Реестр скелетных моделей.
Функции: Загружает и хранит ViewmodelDefinition из ресурсов.

## Inventory System (NEW)
### com.za.zenith.world.inventory.ItemInventory
Назначение: Реализация `IInventory` для предметов-контейнеров (рюкзаки, мешочки).
Функции: Позволяет предмету (`ItemStack`) хранить внутри себя другие предметы, поддерживает динамический размер из `BagComponent`, запрещает вложенность рюкзаков.

## Particle & Shard System (v1.0 NEW)
### com.za.zenith.world.particles
- **Particle.java**: Базовый абстрактный класс для всех визуальных эффектов. Хранит позицию, скорость, 2D-вращение (roll), прозрачность и логику затухания. Не имеет физических коллизий для максимальной стабильности.
- **ShardParticle.java**: Реализация классического воксельного осколка. Использует технику Snippet UV (вырезка 4x4 пикселя из текстуры материала) и билбординг. Поддерживает тинтовку биома.
- **ParticleManager.java**: Глобальный синглтон управления частицами. 
    - `spawnImpact`: Создает контекстные частицы при ударе по Weak Spot. Количество осколков динамически масштабируется от `miningDamage`.
    - `spawnShatter`: Эффект полного разрушения блока.
    - Реализует умный резолв текстур для технических стадий (felling logs) и гибридную логику для дерна (`grass_block`).

### com.za.zenith.engine.graphics
- **ColorProvider.java**: Single Source of Truth для биомных цветов. Содержит статические методы доступа к цветам травы и листвы.
- **ParticleRenderer.java**: Высокопроизводительный инстанс-рендерер.
 Отрисовывает квадратные билборды, ориентированные на камеру. Передает параметры времени, текстурных слоев и UV-смещений в шейдеры.

## Asset Map
