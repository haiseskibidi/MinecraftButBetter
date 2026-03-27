# Code Map: MinecraftButBetter

## Asset Map
### src/main/resources/minecraft/textures
Назначение: Полный набор оригинальных ассетов Minecraft.
- **block/**: Текстуры блоков (16x16).
- **item/**: Иконки предметов.
- **gui/**: Элементы интерфейса (widgets.png, icons.png).
- **font/**: Текстуры шрифтов (ascii.png, unicode_page_04.png).

### src/main/resources/minecraft/registry
- **physics.json**: Конфигурация физических констант (гравитация, скорость паркура).

## Animation & Locomotion System (v4.0 UPDATED)
### src/main/resources/minecraft/animations/
Назначение: Директория со всеми профилями анимаций (JSON). Новые профили: `landing.json` (импульсы приземления), `falling.json` (напряжение при падении).

### com.za.minecraft.entities.Player (UPDATED)
Назначение: Основная сущность игрока с AAA-системой локомоции.
Функции:
- `update()`: Обновляет физические таймеры локомоции на частоте 170Hz для стабильности.
- `updateAnimations()`: Визуальный проход (на частоте кадров). Рассчитывает оффсеты, применяет импульсы приземления (`landingScale`, `landingSide`), эффект падения (`fallingTimer`) и наклон камеры (lean).
- `isFirstFrame`: Флаг для механизма Spawn Settle (предотвращение глитчей при спавне).

### com.za.minecraft.entities.parkour.animation.AnimationProfile
Назначение: Универсальный профиль движения.
Функции: `evaluate(trackName, t, multiplier)` — рассчитывает значение с учетом `mirror` свойства трека и переданного множителя.

### com.za.minecraft.entities.parkour.animation.AnimationTrack
Назначение: Поток данных для одного параметра.
Поля: `mirror` (вместо `mirrored`) — флаг инвертирования оси при зеркалировании.

### com.za.minecraft.entities.parkour.animation.Keyframe
Назначение: Точка анимации (время, значение, тип интерполяции).

## Inventory System (v3) (NEW)
### com.za.minecraft.world.inventory.IInventory
Назначение: Универсальный интерфейс для любого объекта, способного хранить ItemStack.
Функции: getStack(int), setStack(int, ItemStack), size(), isItemValid(int, ItemStack), isSlotActive(int)

### com.za.minecraft.world.inventory.RegistryInventory
Назначение: Виртуальный инвентарь, предоставляющий доступ ко всем зарегистрированным предметам (для Creative/Developer Panel).

### com.za.minecraft.entities.inventory.Slot
Назначение: Логическая обертка над конкретным индексом в IInventory. Поддерживает кастомную валидацию.
Функции: getStack(), setStack(ItemStack), isItemValid(ItemStack), withValidator(Predicate)

### com.za.minecraft.entities.inventory.SlotGroup
Назначение: Группа слотов с общим поведением и условием активации (activeSupplier).
Функции: isActive(), addSlot(Slot), withActiveSupplier(Supplier)

## UI System (UPDATED)
### com.za.minecraft.engine.graphics.ui.Screen
Назначение: Базовый интерфейс для всех игровых экранов (`init`, `render`, `handleMouseClick`, `handleScroll`).

### com.za.minecraft.engine.graphics.ui.ScrollPanel
Назначение: Модульный компонент для скроллинга. Использует `glScissor` для аппаратного отсечения контента.

### com.za.minecraft.engine.graphics.ui.JournalScreen
Назначение: Реализация планшета выжившего. Отображает категории, статьи и динамические рецепты.

## Journal System (NEW)
### com.za.minecraft.world.journal.JournalRegistry
Назначение: Реестр категорий и записей дневника.

### com.za.minecraft.world.journal.JournalEntry / JournalCategory / JournalElement
Назначение: Модели данных для структуры контента планшета.

## Item System (UPDATED)
### com.za.minecraft.world.items.Item (UPDATED)
Назначение: Базовый класс предмета.
Логика: Добавлена поддержка `miningSpeed` и `maxStackSize` напрямую в класс для Data-Driven настройки эффективности и вместимости стаков.

### com.za.minecraft.world.DataLoader (UPDATED)
Назначение: Загрузчик ресурсов.
Логика: Добавлена поддержка парсинга `miningSpeed` и `maxStackSize` из JSON предметов, а также загрузка всей структуры Journal.

## GUI System (UPDATED)
### com.za.minecraft.engine.graphics.ui.Screen
Назначение: Базовый интерфейс для всех игровых экранов.

### com.za.minecraft.engine.graphics.ui.ScrollPanel
Назначение: Универсальный компонент прокрутки с поддержкой Scissor Test.

### com.za.minecraft.engine.graphics.ui.JournalScreen
Назначение: Планшет выжившего. Отображает динамический контент из JSON.

### com.za.minecraft.engine.graphics.ui.ScreenManager
Назначение: Синглтон для управления активным экраном инвентаря.
Функции: openPlayerInventory(player, sw, sh), openChest(container, playerInv, sw, sh), closeScreen(), getActiveScreen(), render(...)

### com.za.minecraft.engine.graphics.ui.InventoryScreen
Назначение: Базовый класс для всех окон с поддержкой слотов.
Функции: init(sw, sh), render(renderer, sw, sh, atlas), getSlotAt(mx, my)

### com.za.minecraft.engine.graphics.ui.PlayerInventoryScreen
Назначение: Основной экран инвентаря игрока. Строится динамически на основе JSON-конфига.

### com.za.minecraft.engine.graphics.ui.ChestScreen
Назначение: Универсальный экран для контейнеров (сундуков) с сеткой 9xN.

### com.za.minecraft.engine.graphics.ui.SlotUI
Назначение: Визуальное представление слота на экране.
Функции: getX(), getY(), getSlot(), isMouseOver(mx, my, size)

### src/main/resources/shaders/ui_fragment.glsl (UPDATED)
Назначение: Пиксельный шейдер для элементов интерфейса.
Логика: Поддержка `isGrayscale` униформы для обесцвечивания текстур плейсхолдеров.

### com.za.minecraft.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Главный рендерер GUI.
Функции: renderSlot(x, y, size, stack, placeholder, sw, sh, atlas).
Логика: При отрисовке плейсхолдера включает `isGrayscale = 1` и применяет альфу 0.4.

### com.za.minecraft.entities.inventory.Slot (UPDATED)
Назначение: Логический слот.
Новые поля: placeholderTexture, type (String).
Логика: `isItemValid` проверяет гибридную строгую валидацию (разрешает в `any` или при совпадении типов).

### com.za.minecraft.engine.graphics.ui.GUIRegistry
Назначение: Реестр загруженных конфигураций GUI.

## Core Engine
### com.za.minecraft.Application
Назначение: Точка входа в приложение, парсинг аргументов командной строки.
Функции: main(String[] args)
Зависимости: com.za.minecraft.engine.core.GameLoop

### com.za.minecraft.engine.core.GameLoop
Назначение: Главный игровой цикл, управление состояниями игры (пауза, инвентарь), связь между системами.
Функции: getInstance(), toggleInventory(), togglePause(), getPlayer(), getWorld(), getCamera(), runSingleplayer(), runAsHost(String name), runAsClient(String name, String address), init(), loop(), input(), update(float interval), render(), cleanup()
Зависимости: Window, Timer, Camera, InputManager, Renderer, World, Player, Hotbar, GameServer, GameClient

### com.za.minecraft.engine.core.Window
Назначение: Управление окном GLFW и контекстом OpenGL.
Функции: init(), update(), cleanup(), shouldClose(), isKeyPressed(int keyCode)
Зависимости: GLFW, GL

### com.za.minecraft.engine.core.Timer
Назначение: Подсчет времени между кадрами (delta time).
Функции: updateDelta(), getDelta(), getDeltaF()

### com.za.minecraft.engine.core.GameMode
Назначение: Перечисление режимов игры (SINGLEPLAYER, MULTIPLAYER_HOST, MULTIPLAYER_CLIENT).

### com.za.minecraft.engine.input.InputManager (UPDATED)
Назначение: Обработка ввода и диспетчеризация действий.
Логика: Полностью де-хардкожена. Делегирует взаимодействие с блоками через `blockDef.onUse`.
Функции: input(), handleInventoryClick(window, button), handleDevPanelClick(mx, my), calculateMetadata(type, normal, hitPoint, camera), needsPreview(type), getSlotAt(), dropStack(), getHoveredSlot(), getDraggedSlots(), clearHeldStack()
Зависимости: Window, Camera, Player, World, BlockRegistry

### com.za.minecraft.world.blocks.entity.ITickable (UPDATED)
Назначение: Интерфейс для обновляемых сущностей.
Функции: Метод `update`, добавлен `shouldTick()` для оптимизации (Lazy Ticking).

### com.za.minecraft.engine.graphics.Texture (UPDATED)
Назначение: Управление OpenGL текстурами.
Функции: Загрузка через STB, генерация мипмапов. Добавлен fallback `generateMissingTexture` (пурпурно-черная шахматка).

## Graphics
### com.za.minecraft.engine.graphics.Renderer (UPDATED)
Назначение: Координация всех процессов отрисовки.
Логика: Реализует **интерполяцию кадров** (Alpha Lerp) между `prevPosition` и текущей позицией для устранения дрожания при движении. Обновляет анимации в `render` проходе для субфреймовой плавности.
Функции: render(alpha), renderViewModel(alpha)
Зависимости: Shader, Mesh, TextureAtlas, Framebuffer, PostProcessor, UIRenderer, DebugRenderer

### com.za.minecraft.engine.graphics.Camera (UPDATED)
Назначение: Управление вектором взгляда.
Логика: Поддерживает `prevPosition` и методы интерполяции для плавного движения камеры независимо от UPS.
Функции: getViewMatrix(), getProjectionMatrix(), moveRotation(float rx, float ry, float rz), updateAspectRatio(float ratio), setOffsets(float x, float y, float z)

### com.za.minecraft.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Отрисовка 2D элементов (прицел, хотбар, инвентарь, меню паузы).
Логика: Поддержка 32-битных ID предметов в кэше текстур. Гарантирует правильный Z-order (Held Stack и Tooltips рисуются последними).
Функции: init(), renderCrosshair(int sw, int sh), renderHotbar(int sw, int sh, DynamicTextureAtlas atlas), renderInventory(int sw, int sh, DynamicTextureAtlas atlas), renderItemIcon(Item item, int x, int y, float size, int sw, int sh, DynamicTextureAtlas atlas), renderDeveloperPanel(...)
Зависимости: Shader, Texture, FontRenderer, ItemRegistry, BlockTextureMapper

### com.za.minecraft.engine.graphics.ui.Hotbar
Назначение: Логика выбора слотов в хотбаре и их позиционирования на экране.
Функции: setSelectedSlot(int slot), getStackInSlot(int slot), getSelectedItemStack()
Зависимости: Player, Inventory, ItemStack

### com.za.minecraft.engine.graphics.DynamicTextureAtlas / DynamicTextureArray
Назначение: Управление массивом текстур блоков (GL_TEXTURE_2D_ARRAY).
Функции: 
- Автоматическая сборка слоев (layers) из всех зарегистрированных блоков (16x16).
- Поддержка анизотропной фильтрации (4x) и настройки LOD_BIAS (-0.5).
- Устранение артефактов мерцания (shimmering) и наложения краев (edge bleeding).
- Предоставление индекса слоя (W-компонента) через `BlockTextureMapper`.

## World & Blocks
### com.za.minecraft.world.World (UPDATED)
Назначение: Управление чанками и глобальное хранилище блоков/сущностей.
Логика: Автоматическая очистка удаленных сущностей (`removed` флаг) в `update()`.
Функции: getBlock(int x, int y, int z), setBlock(int x, int y, int z, int blockType), getLoadedChunks(), spawnEntity(Entity), update(float deltaTime), getNoiseLevelAt(Vector3f pos)
Зависимости: Chunk, TerrainGenerator, ChunkPos, Entity

### com.za.minecraft.world.chunks.Chunk (UPDATED)
Назначение: Хранилище данных о блоках 16x384x16.
Оптимизация: Использует одномерный массив `int[] blockData` с упаковкой (Type << 8 | Metadata) для снижения GC и экономии RAM.
Функции: getBlock(x, y, z), setBlock(x, y, z, block), getRawBlockData(x, y, z)

### com.za.minecraft.world.blocks.BlockRegistry (UPDATED)
Назначение: Центральный реестр определений блоков на базе `NumericalRegistry`.
Функции: registerBlock(BlockDefinition def), getBlock(int id), getBlock(Identifier id), getTextures(int id), allTextureKeys()

### com.za.minecraft.world.blocks.Blocks (NEW)
Назначение: Класс-холдер статических ссылок на базовые блоки.
Инициализация: **Reflection**. Поля (GRASS_BLOCK, STONE и т.д.) заполняются автоматически при запуске, сопоставляя имена с Identifier.

### com.za.minecraft.world.blocks.BlockDefinition (UPDATED)
Назначение: Базовый класс для всех определений блоков.
Функции: Хранит свойства (solid, transparent, hardness), обрабатывает взаимодействие через `onUse` и `onLeftClick`. Поддерживает флаг `tinted` (биомовое окрашивание).
Зависимости: Identifier, VoxelShape, World, Player.

### com.za.minecraft.world.blocks.CampfireBlockDefinition (NEW)
Назначение: Логика костра.
Функции: Обработка жарки сырого мяса (`RAW_MEAT` -> `COOKED_MEAT`) через `onUse`.

### com.za.minecraft.world.blocks.PitKilnBlockDefinition (NEW)
Назначение: Логика ямного обжига.
Функции: Управление наполнением ямы бревнами и запуск обжига через огниво.

### com.za.minecraft.world.blocks.UnfiredVesselBlockDefinition (NEW)
Назначение: Логика сырого сосуда.
Функции: Превращение в `PIT_KILN` при использовании соломы (`STRAW`) на блоке.

### com.za.minecraft.world.blocks.LogBlockDefinition (UPDATED)
Назначение: Логика бревна.
Функции: Превращение в `STUMP` при ударе каменным топором. Реализован триггер Treecapitator: при разрушении "натурального" бревна (флаг 0x80 в метаданных) оно заменяется на `FELLING_STAGE_1` с сохранением типа дерева.

### com.za.minecraft.world.blocks.FellingLogBlockDefinition (UPDATED)
Назначение: Универсальная логика стадий срубания дерева.
Логика: Использует `getNextStage()` из JSON для перехода (1->2->3->4). На финальной стадии (где `next_stage` отсутствует) запускает снос всего дерева.

### com.za.minecraft.world.blocks.WoodTypeRegistry (UPDATED)
Назначение: Реестр для маппинга типов древесины.
Функции: Загружается из `wood_types.json`. Сопоставляет числовые индексы (0-255) с `Identifier` оригинальных бревен. Используется для хранения типа дерева в метаданных универсальных стадий и динамического выбора типа при генерации.

### com.za.minecraft.world.TreecapitatorService (NEW)
Назначение: Сервис для реализации механики Treecapitator.
Функции: `fellTree(world, pos, player)` — запускает BFS-поиск всех связных блоков (логи и листва) с тегом `treecapitator` и флагом `BIT_NATURAL`. Консолидирует дроп в одну точку.

### com.za.minecraft.world.blocks.BlockTextureMapper (UPDATED)
Назначение: Сопоставление блоков и их состояний с текстурами в атласе.
Логика: 
- Динамически подставляет текстуры для стадий срубания на основе тега `minecraft:logs`.
- **Важно**: Использует маску `& 0x07` для чтения направления, игнорируя флаг `BIT_NATURAL`, что исправляет отображение текстуры среза (колец) на верхней/нижней грани.
- Для стадий `felling_log` извлекает `woodIndex` из метаданных и запрашивает оригинальный лог из `WoodTypeRegistry` для подстановки текстур.

### com.za.minecraft.world.blocks.BlockTypeRegistry (NEW)
Назначение: Реестр фабрик для различных типов блоков.
Функции: Маппинг строкового `type` из JSON в конструкторы подклассов `BlockDefinition`.

### com.za.minecraft.world.blocks.Block (UPDATED)
Назначение: Легковесный экземпляр блока в мире.
Поля: type (int), metadata (byte).
Функции: getType(), getMetadata(), isSolid(), isTransparent(), isAir(), getShape()

### com.za.minecraft.world.blocks.entity.BlockEntity (NEW)
Назначение: Базовый класс для блоков с состоянием (энергия, инвентарь).
Функции: setRemoved(), isRemoved(), getPos(), setWorld(World)

### com.za.minecraft.world.blocks.entity.IEnergyStorage (NEW)
Назначение: Стандарт для всех энергозависимых блоков.
Методы: receiveEnergy, extractEnergy, getEnergyStored

### com.za.minecraft.world.blocks.entity.GeneratorBlockEntity (UPDATED)
Назначение: Логика бензогенератора. Реализует IEnergyStorage. Вырабатывает энергию из топлива (FuelComponent).

### com.za.minecraft.world.blocks.entity.CableBlockEntity (NEW)
Назначение: Передача энергии между соседними хранилищами (система сообщающихся сосудов).

### com.za.minecraft.world.blocks.entity.LampBlockEntity (NEW)
Назначение: Потребитель энергии (излучает свет при наличии питания).

### com.za.minecraft.world.blocks.entity.BatteryBlockEntity (NEW)
Назначение: Хранилище большого объема энергии (10 000 ед.). Реализует IEnergyStorage.

### com.za.minecraft.world.blocks.entity.ITickable (NEW)
Назначение: Интерфейс для обновления сущностей блоков каждый тик.

## Generation & Structures (UPDATED)
### com.za.minecraft.world.generation.TerrainGenerator
Назначение: Координация Generation Pipeline.
Шаги: CityLayoutStep, BuildingGeneratorStep, OvergrowthStep, ScavengeDecorationStep.

### com.za.minecraft.world.generation.pipeline.steps.OvergrowthStep (UPDATED)
Назначение: Зарастание руин растительностью.
Логика: Генерирует короткую и высокую траву, деревья. Для высокой травы ставит два блока с разными метаданными (низ и верх) согласно PlacementType.DOUBLE_PLANT.

### com.za.minecraft.world.generation.pipeline.steps.ScavengeDecorationStep (NEW)
Назначение: Data-Driven спавн 3D ресурсов на поверхности мира на основе `scavenge.json`.

### com.za.minecraft.world.generation.structures.StructureRegistry (NEW)
Назначение: Реестр динамически загруженных шаблонов структур из JSON.

### com.za.minecraft.world.generation.structures.PrefabManager (UPDATED)
Назначение: Статические ссылки на шаблоны зданий (RUINED_HOUSE_1, SMALL_STORE и т.д.).
Инициализация: **Reflection** из `StructureRegistry`.

### com.za.minecraft.world.generation.structures.StructureTemplate (UPDATED)
Назначение: Шаблон структуры. Хранит данные в упакованном `int[][][]`.
Функции: build(World, x, y, z), parse(layers, palette)

## Items System (UPDATED)
### com.za.minecraft.world.items.Item (UPDATED)
Назначение: Базовый класс для всех предметов. Поддерживает `DataComponents` и `visualScale`.
Поля: id (int), identifier, components, visualScale, weight.
Функции: addComponent(Class, Component), getComponent(Class), hasComponent(Class), getMiningSpeed(blockType)

### com.za.minecraft.world.items.ItemRegistry (UPDATED)
Назначение: Центральный реестр всех предметов на базе `NumericalRegistry`.
Функции: registerItem(Item item), getItem(int id), getItem(Identifier id), getAllItems()

### com.za.minecraft.world.items.ItemMeshGenerator (NEW)
Назначение: Процедурная генерация 3D моделей из 2D текстур (с толщиной) для рендеринга предметов в руке и в мире.
Функции: generateItemMesh(texturePath, atlas, itemId)

### com.za.minecraft.world.items.Items (NEW)
Назначение: Класс-холдер статических ссылок на базовые предметы.
Инициализация: **Reflection**.

### com.za.minecraft.world.items.ItemStack
Назначение: Контейнер для предмета в инвентаре (хранит количество и текущую прочность).
Функции: getItem(), getCount(), getDurability(), copy(), split(int amount), isStackableWith(ItemStack)

### com.za.minecraft.world.items.component.ItemComponent (NEW)
Назначение: Базовый интерфейс для свойств предметов.
Реализации: `FoodComponent` (питание), `ToolComponent` (эффективность, прочность), `FuelComponent` (топливо).

## Recipes System (NEW)
### com.za.minecraft.world.recipes.IRecipe
Назначение: Базовый интерфейс для всех рецептов.
Функции: getResult(), getType(), getId()

### com.za.minecraft.world.recipes.RecipeRegistry
Назначение: Центральное хранилище всех рецептов, загруженных из JSON.
Функции: register(IRecipe), getRecipesByType(String type)

### com.za.minecraft.world.recipes.NappingRecipe
Назначение: Рецепт для механики скалывания камней (Tier 1).
Поля: inputId, result, pattern (5x5 boolean array).

### com.za.minecraft.world.recipes.CarvingRecipe (NEW)
Назначение: Data-Driven рецепт для обтёсывания блоков (бревно -> пень).
Поля: inputBlock, tool, intermediateBlock, resultBlock.

## Entities & Physics (UPDATED)
### com.za.minecraft.entities.Entity (UPDATED)
Назначение: Базовый физический объект.
Поля: position, velocity, rotation, boundingBox, removed (NEW).
Функции: update(float delta, World world), move(World world, dx, dy, dz), getPosition(), getVelocity(), setRemoved(), isRemoved()

### com.za.minecraft.entities.LivingEntity (NEW)
Назначение: Сущность со здоровьем (Игрок, Мобы). Наследует Entity.
Функции: takeDamage(float amount), heal(float amount), isDead()

### com.za.minecraft.entities.Player
Назначение: Сущность игрока.
Функции: update(float delta, World world), jump(), swing(), addNoise(float), getNoiseLevel(), getHunger(), eat(Item)

### com.za.minecraft.entities.ResourceEntity (NEW)
Назначение: Легкая статичная сущность для отображения 3D предметов на земле (Foraging).

### com.za.minecraft.entities.ScoutEntity (NEW)
Назначение: Зараженный-скаут. ИИ логика слуха и погони.

### com.za.minecraft.entities.ItemEntity (NEW)
Назначение: Сущность выброшенного предмета в мире. Поддерживает физику и вращение.

### com.za.minecraft.world.physics.AABB (UPDATED)
Назначение: Физическая оболочка объекта.
Функции: intersects(AABB), contains(Vector3f), offset(x, y, z), intersectDist(origin, direction) (NEW - для рейкаста)

### com.za.minecraft.world.physics.Raycast (UPDATED)
Назначение: Алгоритм прослеживания луча.
Функции: raycast(World, origin, direction) (блоки), raycastEntity(World, origin, direction) (NEW - сущности)

## Networking (UPDATED)
### com.za.minecraft.network.GameServer / GameClient
Назначение: Мультиплеер на базе Kryonet.
Логика: Поддержка 32-битных ID блоков в пакетах `BlockUpdatePacket`.

## Utilities (UPDATED)
### com.za.minecraft.utils.Identifier (NEW)
Назначение: Единый стандарт именования `namespace:path`.

### com.za.minecraft.utils.TextureAABBGenerator (NEW)
Назначение: Автоматическая генерация AABB на основе прозрачности текстуры.
Функции: Попиксельное сканирование, вычисление границ, кеширование.

### com.za.minecraft.entities.EntityRegistry (NEW)
Назначение: Реестр определений сущностей (EntityDefinition) из JSON.

### com.za.minecraft.entities.DecorationEntity (NEW)
Назначение: Универсальная сущность для 3D декораций (бревна, камни) на основе JSON.

### com.za.minecraft.world.blocks.entity.StumpBlockEntity (NEW)
Назначение: Сохранение предмета и прогресса ударов для механики крафта на пне.

### com.za.minecraft.world.blocks.StumpBlockDefinition (NEW)
Назначение: Обработка логики кликов (положить/забрать/ударить) по пню.

### com.za.minecraft.world.blocks.entity.PitKilnBlockEntity (NEW)
Назначение: Управление процессом обжига глины (таймер, превращение).

### com.za.minecraft.utils.I18n
Назначение: Система локализации на базе JSON-файлов.
Зависимости: Gson
