# Code Map: MinecraftButBetter

## Asset Map
### src/main/resources/minecraft/textures
Назначение: Полный набор оригинальных ассетов Minecraft.
- **block/**: Текстуры блоков (16x16).
- **item/**: Иконки предметов.
- **gui/**: Элементы интерфейса.
    - `crosshair.png`: Прицел.
- **entity/**: Текстуры сущностей.

### src/main/resources/minecraft/gui
Назначение: Конфигурации игровых интерфейсов в формате JSON.
- `player_inventory.json`: Главное окно игрока.
- `hotbar.json`: Конфигурация HUD-хотбара.
- `chest.json`: Интерфейс сундуков.

## UI & Animation Editor (v1.1 MODULAR)
### com.za.minecraft.engine.graphics.ui.editor.animation
- **AnimationEditorScreen.java**: Главный оркестратор. Делегирует задачи специализированным модулям.
- **AnimationEditorState.java**: Модель данных. Хранит ключи, треки, текущее время и выбранные объекты.
- **AnimationEditorRenderer.java**: Рендеринг 3D сцены студии, отрисовка Гизмо и "призраков" кадров. Очищен от логики IK.
- **EditorInputHandler.java**: Логика взаимодействия: Ray-OBB Picking, Grab/Rotate манипуляции через чистый FK, горячие клавиши.
- **EditorUI.java**: Отрисовка всех панелей интерфейса (Parts, Properties, Timeline). Удален статус IK и управление таргетами.
- **AnimationExporter.java**: Конвертация состояния в JSON формат движка.
- **TransformController.java**: Низкоуровневая логика трансформаций. Реализует перемещение и вращение костей в 3D пространстве с учетом иерархии.

### com.za.minecraft.engine.graphics.ui.InventoryScreen (UPDATED)
Назначение: Базовый класс для всех экранов с инвентарями.
Функции: Реализует метод `handleQuickMove` — универсальную Data-Driven логику для Shift+Click, использующую правила из JSON. Содержит абстрактный метод `getScreenIdentifier()`.

### com.za.minecraft.engine.graphics.ui.ChestScreen (UPDATED)
Назначение: Универсальный экран для любых контейнеров.
Функции: Принимает динамический `Identifier guiId` в конструкторе. Загружает разметку из соответствующего JSON файла через `InventoryLayout`. Поддерживает отображение нескольких инвентарей (контейнер + игрок).

### com.za.minecraft.engine.graphics.ui.InventoryLayout (UPDATED)
Назначение: Движок динамической разметки интерфейсов.
Функции: Генерирует позиции слотов и групп на основе JSON. Поддерживает `Map<String, IInventory>` для работы с несколькими источниками данных одновременно. Реализует логику `relativeTo` (относительное позиционирование групп).

### com.za.minecraft.engine.graphics.ui.SlotUI (UPDATED)
Назначение: Визуальное представление слота.
Функции: Содержит `groupId` для связи слота с правилами быстрого перемещения в JSON конфигурации.

### com.za.minecraft.engine.graphics.ui.GUIConfig (UPDATED)
Назначение: Модель данных для JSON-конфигураций интерфейса.
Функции: Расширена полями `inventorySource`, `startIndex`, `slotsCount` и `quickMoveTo` для каждой группы.

### com.za.minecraft.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Модульный рендерер интерфейса (Facade).
Функции: Является точкой входа для рендеринга всего UI, но делегирует работу специализированным суб-рендерерам: `UIPrimitives` (базовые формы), `SlotRenderer` (предметы/слоты), `HUDRenderer` (игровой оверлей, стамина, голод), `InventoryScreenRenderer` (инвентарь и dev-панель) и `MenuRenderer`.

## World & Items (UPDATED)
### com.za.minecraft.world.World (UPDATED)
Назначение: Управление состоянием мира, сущностями и чанками.
Функции: Хранит `blockDamageMap` с объектами `BlockDamageInstance` (урон + история шрамов). Реализует логику регенерации блоков и постепенного удаления шрамов в методе `update()`.

### com.za.minecraft.world.actions (NEW)
Назначение: Data-Driven система действий игрока (бег, прыжок, добыча, паркур).
Функции: `ActionDefinition` парсит JSON-файлы с параметрами шума, выносливости и голода. `ActionRegistry` хранит их для использования в `Player` и `ParkourHandler`.

### com.za.minecraft.world.blocks.BlockDefinition (UPDATED)
Назначение: Физические и визуальные свойства блока.
Функции: Хранит `interaction_cooldown` и `healing_speed` (скорость регенерации).

### com.za.minecraft.engine.input.MiningController (UPDATED)
Назначение: Контроллер процесса добычи блоков.
Функции: Синхронизирует прогресс разрушения и историю ударов (`hitHistory`) с данными из `World`. Обрабатывает механику `drop_on_hit` (выпадение лута при ударе) и штрафы к прочности.

### com.za.minecraft.world.items.Item (UPDATED)
Назначение: Базовый класс предмета.
Функции: Управление компонентами и индивидуальным `interaction_cooldown`.

## Entity System (v5.6 NEW)
### com.za.minecraft.entities.Entity (UPDATED)
Назначение: Базовый класс для всех сущностей (игрок, мобы, предметы).
Функции: Реализует интерполяцию (`prevPosition`, `prevRotation`), систему выталкивания из блоков (`Unstuck`) и общую логику перемещения `move()` с коллизиями.

### com.za.minecraft.entities.ItemEntity (UPDATED)
Назначение: Сущность предмета, выброшенного в мир.
Функции: Реализует `Ground Lock` (отключение гравитации на земле), стабилизацию вращения (выравнивание плашмя при приземлении) и кастомную гравитацию на основе веса.

### com.za.minecraft.entities.ResourceEntity (NEW)
Назначение: Статичная 3D модель ресурса (палка, камень) на поверхности.
Функции: Используется для декораций мира, собирается мгновенно ЛКМ.

### com.za.minecraft.engine.graphics.Shader (UPDATED)
Назначение: Обертка над шейдерной программой.
Функции: Добавлен метод `setVector4f()` для передачи координат и интенсивности шрамов.

### com.za.minecraft.world.World (UPDATED)
Назначение: Управление состоянием мира, сущностями и чанками.
Функции: Хранит `blockDamageMap` с объектами `BlockDamageInstance` (урон + история шрамов в `Vector4f`). Реализует логику задержки регенерации (5 сек) и плавного затухания шрамов (изменение компоненты `w`).

### com.za.minecraft.engine.graphics.Renderer (UPDATED)
Назначение: Главный контроллер рендеринга.
Функции: Управляет проходами отрисовки. Реализован метод `renderPersistentScars` для отображения повреждений всех блоков мира с использованием `glPolygonOffset`. Исправлено выравнивание меша `ItemEntity`.

### com.za.minecraft.engine.graphics.vfx.MiningVFXManager (NEW)
Назначение: Менеджер визуальных эффектов добычи.
Функции: Рассчитывает уровень раскаления (`heatLevel`) инструментов и рук. Обеспечивает плавное остывание и универсальный сброс прогресса добычи при смене/выбрасывании предмета (через отслеживание `identityHashCode` и слота).

### src/main/resources/shaders (NEW SHADERS)
- **viewmodel_vertex/fragment.glsl**: Изолированные шейдеры для рук и предметов. Поддерживают процедурное раскаление (`uMiningHeat`), маскировку по весам костей и поддержку биомного тинта для блоков в руках.
- **crosshair_vertex/fragment.glsl**: Шейдеры динамического прицела. Поддерживают data-driven анимации разлета (`spreadScale`) и отдачи (`recoilScale`).

### com.za.minecraft.engine.graphics.ui.crosshair (UPDATED)
- **CrosshairDefinition**: Добавлены поля `recoilScale` и `spreadScale` для настройки анимаций в JSON.
- **CrosshairManager**: Управляет состояниями. Приоритет `MINING` восстановлен для корректного отображения на интерактивных блоках (пни).
- **CrosshairRenderer**: Теперь строго использует специализированный шейдер для отрисовки матриц из JSON. Реализует анимацию разлета элементов.

### com.za.minecraft.engine.graphics.model (UPDATED)
- **ViewmodelRenderer**: Теперь принимает `heat` и распределяет его между руками и инструментами. Доступен геттер для `heldItemRenderer`.
- **HeldItemRenderer**: Реализует точечную передачу уровня жара инструменту. Метод `getOrGenerateMesh` сделан публичным для системы выбора.

## Animation & Locomotion System (v4.5 UPDATED)
### com.za.minecraft.entities.Player (UPDATED)
Назначение: Главная сущность игрока.
Функции: Управление инвентарем, статами (голод, стамина), паркуром и анимациями. Поддерживает методы `swing()` (удар) и `interact()` (быстрый сбор/подбор). Поддерживает методы `swing()` (удар) и `interact()` (быстрый сбор/подбор).

### com.za.minecraft.engine.input.MiningController (NEW)
Назначение: Контроллер процесса добычи блоков.
Функции: Управляет таймерами (cooldown, breakingDelay), генерирует Weak Spots, рассчитывает прогресс разрушения. Автоматически выбирает тип анимации (`swing` vs `interact`) на основе прочности блока. Передает данные для отрисовки прокси-блока в `Renderer`.

### com.za.minecraft.engine.core.GameLoop (UPDATED)
Назначение: Главный цикл. Добавлена поддержка переключения в режим Студии (F8) и изоляция обновлений.

## Physical Viewmodel System (v5.0 NEW)
### com.za.minecraft.engine.graphics.Mesh (UPDATED)
Назначение: Низкоуровневая обертка над VBO/VAO.
Функции: Отрисовка, очистка ресурсов. Добавлены методы `getMin()` и `getMax()` для вычисления Bounding Box меша в реальном времени.

### com.za.minecraft.engine.graphics.model.ViewmodelRenderer (UPDATED)
Назначение: Рендерер рук игрока.
Функции: Рендерит иерархия костей. Делегирует отрисовку удерживаемых предметов классу `HeldItemRenderer`.

### com.za.minecraft.engine.graphics.model.HeldItemRenderer (NEW)
Назначение: Специализированный рендерер для предметов и блоков в руках.
Функции: Управляет трансформациями (смещение, поворот, масштаб) относительно костей кисти. Реализует динамическое прижатие блоков к ладони на основе их геометрии.

### com.za.minecraft.world.items.ItemMeshGenerator (UPDATED)
Назначение: Генератор 3D мешей из 2D текстур.
Функции: Использует PCA (Principal Component Analysis) для автоматического определения ориентации предмета и точки хвата. Генерирует вертикально выровненные меши.

### com.za.minecraft.engine.graphics.model.ViewmodelPhysics
Назначение: Физический симулятор для рук и предметов.
Функции: Решает дифференциальные уравнения 2-го порядка (пружина-масса-демпфер) для расчета инерции и веса.

### com.za.minecraft.engine.graphics.model.ViewmodelController
Назначение: Менеджер скелетных анимаций.
Функции: Применяет AnimationProfile к костям скелета (поддержка парсинга треков `nodeName:track`), поддерживает наслоение (blending) нескольких анимаций и динамическое масштабирование времени (`baseMiningCooldown`).

### com.za.minecraft.engine.graphics.model.ViewmodelMeshGenerator
Назначение: Генератор воксельных мешей для костей.
Функции: Создает оптимизированную геометрию на основе кубов, описанных в JSON.

### com.za.minecraft.engine.graphics.model.ik (NEW)
- **FABRIKSolver.java**: Математическое ядро IK. Реализует алгоритм FABRIK с поддержкой Pole Targets для контроля сгибов.
- **IKChain.java**: Контейнер для цепочки костей, хранит целевые позиции и ограничения.
- **constraints/IKConstraint.java**: Базовый интерфейс для ограничений суставов.
- **constraints/HingeConstraint.java**: Ограничение вращения по одной оси (для локтей и коленей).

### com.za.minecraft.engine.graphics.ui.editor.animation (UPDATED)
- **EditorIKManager.java**: Управляет несколькими IK-цепями одновременно. Поддерживает `autoSetup` (руки/ноги) и запекание поз в ключи.
- **AnimationEditorRenderer.java**: Визуализирует цели IK (желтые) и Pole Targets (синие).
- **TransformController.java**: Теперь поддерживает прямой ввод в IK-таргеты при перемещении эффекторов (кистей/стоп).

### com.za.minecraft.engine.graphics.model.ModelRegistry
Назначение: Реестр скелетных моделей.
Функции: Загружает и хранит ViewmodelDefinition из ресурсов.

## Inventory System (NEW)
### com.za.minecraft.world.inventory.ItemInventory
Назначение: Реализация `IInventory` для предметов-контейнеров (рюкзаки, мешочки).
Функции: Позволяет предмету (`ItemStack`) хранить внутри себя другие предметы, поддерживает динамический размер из `BagComponent`, запрещает вложенность рюкзаков.

