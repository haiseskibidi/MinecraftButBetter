# Code Map: MinecraftButBetter

## Asset Map
### src/main/resources/minecraft/textures
Назначение: Полный набор оригинальных ассетов Minecraft.
- **block/**: Текстуры блоков (16x16).
- **item/**: Иконки предметов.
- **gui/**: Элементы интерфейса (widgets.png, icons.png).
- **font/**: Текстуры шрифтов (ascii.png, unicode_page_04.png).

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
Назначение: Обработка ввода и управление взаимодействием.
Логика: Полностью де-хардкожена. Поддерживает сбор 3D ресурсов (`ResourceEntity`) и универсальную установку через `PlacementType`.
Функции: input(), handleInventoryClick(window, button), calculateMetadata(type, normal, hitPoint, camera), needsPreview(type), getSlotAt(), dropStack(), getHoveredSlotIndex(), getDraggedSlots(), clearHeldStack()
Зависимости: Window, Camera, Player, World, BlockRegistry

## Graphics
### com.za.minecraft.engine.graphics.Renderer (UPDATED)
Назначение: Координация всех процессов отрисовки (мир, превью блоков, View Model, UI).
Логика: Кэширование мешей предметов и блоков для 32-битных ID. Поддержка рендеринга `ResourceEntity` с учетом `visualScale`.
Функции: init(int width, int height), render(Window window, Camera camera, World world, RaycastResult highlightedBlock, GameClient client), renderViewModel(Window, Camera, Player), renderEntities(Camera, World), renderDebug(float fps, int width, int height), setPreviewBlock(BlockPos pos, Block block)
Зависимости: Shader, Mesh, TextureAtlas, Framebuffer, PostProcessor, UIRenderer, DebugRenderer

### com.za.minecraft.engine.graphics.Camera
Назначение: Управление вектором взгляда игрока и матрицами проекции.
Функции: getViewMatrix(), getProjectionMatrix(), moveRotation(float rx, float ry, float rz), updateAspectRatio(float ratio), setOffsets(float x, float y, float z)

### com.za.minecraft.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Отрисовка 2D элементов (прицел, хотбар, инвентарь, меню паузы).
Логика: Поддержка 32-битных ID предметов в кэше текстур.
Функции: init(), renderCrosshair(int sw, int sh), renderHotbar(int sw, int sh, DynamicTextureAtlas atlas), renderInventory(int sw, int sh, DynamicTextureAtlas atlas), renderItemIcon(Item item, int x, int y, float size, int sw, int sh, DynamicTextureAtlas atlas)
Зависимости: Shader, Texture, FontRenderer, ItemRegistry, BlockTextureMapper

### com.za.minecraft.engine.graphics.ui.Hotbar
Назначение: Логика выбора слотов в хотбаре и их позиционирования на экране.
Функции: setSelectedSlot(int slot), getStackInSlot(int slot), getSelectedItemStack()
Зависимости: Player, Inventory, ItemStack

### com.za.minecraft.engine.graphics.TextureAtlas / DynamicTextureAtlas
Назначение: Управление набором текстур блоков в одном объекте.
Функции: getUVs(String key), bind(), addTexture(String key, BufferedImage image)

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
Назначение: Описание свойств типа блока.
Поля: id (int), identifier, requiredTool, dropItem, canSupportScavenge, placementType, hardness, solid, transparent, textures, upperTexture (NEW).
Функции: getId(), getIdentifier(), getRequiredTool(), getDropItem(), canSupportScavenge(), getPlacementType(), getUpperTexture()

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
