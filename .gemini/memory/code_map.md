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

### com.za.minecraft.engine.input.InputManager (UPDATED)
Назначение: Обработка ввода и управление взаимодействием.
Логика: Полностью де-хардкожена. Использует `PlacementType` для логики установки и предпросмотра блоков.
Функции: input(), handleInventoryClick(window, button), calculateMetadata(type, normal, hitPoint, camera), needsPreview(type)
Зависимости: Window, Camera, Player, World, BlockRegistry

## Graphics
### com.za.minecraft.engine.graphics.Renderer (UPDATED)
Назначение: Координация всех процессов отрисовки (мир, превью блоков, View Model, UI).
Оптимизация: Кэширование мешей предметов и блоков для 32-битных ID.
Функции: init(int width, int height), render(...), renderViewModel(Window, Camera, Player), setPreviewBlock(BlockPos pos, Block block)
Зависимости: Shader, Mesh, TextureAtlas, Framebuffer, PostProcessor, UIRenderer

### com.za.minecraft.engine.graphics.Camera
Назначение: Управление вектором взгляда игрока и матрицами проекции.
Функции: getViewMatrix(), getProjectionMatrix(), moveRotation(float rx, float ry, float rz), setOffsets(float x, float y, float z)

### com.za.minecraft.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Отрисовка 2D элементов (прицел, хотбар, инвентарь, меню паузы).
Логика: Поддержка 32-битных ID предметов в кэше текстур.
Функции: init(), renderCrosshair(int sw, int sh), renderHotbar(int sw, int sh, DynamicTextureAtlas atlas), renderInventory(...), renderItemIcon(...)
Зависимости: Shader, Texture, FontRenderer, ItemRegistry, BlockTextureMapper

### com.za.minecraft.engine.graphics.TextureAtlas / DynamicTextureAtlas
Назначение: Управление набором текстур блоков в одном объекте.
Функции: getUVs(String key), bind(), addTexture(String key, BufferedImage image)

## World & Blocks
### com.za.minecraft.world.World (UPDATED)
Назначение: Управление чанками и глобальное хранилище блоков.
Функции: getBlock(int x, int y, int z), setBlock(int x, int y, int z, int blockType), getLoadedChunks()
Зависимости: Chunk, TerrainGenerator, ChunkPos

### com.za.minecraft.world.chunks.Chunk (UPDATED)
Назначение: Хранилище данных о блоках 16x384x16.
Оптимизация: Использует одномерный массив `int[] blockData` с упаковкой (Type << 8 | Metadata) для снижения GC.
Функции: getBlock(x, y, z), setBlock(x, y, z, block), getRawBlockData(x, y, z)

### com.za.minecraft.world.blocks.BlockRegistry (UPDATED)
Назначение: Центральный реестр определений блоков на базе `NumericalRegistry`.
Функции: registerBlock(BlockDefinition def), getBlock(int id), getBlock(Identifier id), getTextures(int id)

### com.za.minecraft.world.blocks.Blocks (NEW)
Назначение: Класс-холдер статических ссылок на базовые блоки.
Инициализация: **Reflection**. Поля (GRASS_BLOCK, STONE и т.д.) заполняются автоматически при запуске.

### com.za.minecraft.world.blocks.BlockDefinition (UPDATED)
Назначение: Описание свойств типа блока.
Поля: id (int), identifier, requiredTool, placementType, hardness, solid, transparent, textures.
Функции: getId(), getIdentifier(), getRequiredTool(), getPlacementType()

### com.za.minecraft.world.blocks.Block (UPDATED)
Назначение: Легковесный экземпляр блока в мире.
Поля: type (int), metadata (byte).
Функции: getType(), getMetadata(), isSolid(), isTransparent(), isAir()

## Generation & Structures (UPDATED)
### com.za.minecraft.world.generation.TerrainGenerator
Назначение: Генерация ландшафта и структур через Generation Pipeline.

### com.za.minecraft.world.generation.structures.StructureRegistry (NEW)
Назначение: Реестр динамически загруженных шаблонов структур из JSON.

### com.za.minecraft.world.generation.structures.PrefabManager (UPDATED)
Назначение: Статические ссылки на шаблоны зданий (RUINED_HOUSE_1, SMALL_STORE и т.д.).
Инициализация: **Reflection**. Наполняется из `StructureRegistry`.

### com.za.minecraft.world.generation.structures.StructureTemplate (UPDATED)
Назначение: Шаблон структуры. Хранит данные в упакованном `int[][][]`.
Функции: build(World, x, y, z), parse(layers, palette)

## Items System (UPDATED)
### com.za.minecraft.world.items.Item (UPDATED)
Назначение: Базовый класс для всех предметов. Поддерживает `DataComponents`.
Поля: id (int), identifier, components.
Функции: addComponent(Class, Component), getComponent(Class), hasComponent(Class)

### com.za.minecraft.world.items.ItemRegistry (UPDATED)
Назначение: Центральный реестр всех предметов на базе `NumericalRegistry`.
Функции: registerItem(Item item), getItem(int id), getItem(Identifier id)

### com.za.minecraft.world.items.Items (NEW)
Назначение: Класс-холдер статических ссылок на базовые предметы.
Инициализация: **Reflection**.

### com.za.minecraft.world.items.component.ItemComponent (NEW)
Назначение: Базовый интерфейс для свойств предметов.
Реализации: `FoodComponent`, `ToolComponent`, `FuelComponent`.

### com.za.minecraft.world.items.ItemStack
Назначение: Контейнер для предмета в инвентаре (хранит количество и текущую прочность).
Функции: getItem(), getCount(), getDurability(), isStackableWith(ItemStack)

## Recipes System (NEW)
### com.za.minecraft.world.recipes.IRecipe
Назначение: Базовый интерфейс для всех рецептов.
Функции: getResult(), getType(), getId()

### com.za.minecraft.world.recipes.RecipeRegistry
Назначение: Центральное хранилище всех рецептов, загруженных из JSON.
Функции: register(IRecipe), getRecipesByType(String type)

### com.za.minecraft.world.recipes.NappingRecipe
Назначение: Рецепт для механики скалывания камней (Tier 1).
Поля: inputId, result, pattern (5x5 boolean array)

## Entities & Physics
### com.za.minecraft.entities.Entity / LivingEntity / Player
Назначение: Физические объекты и существа.
Функции: update(float delta, World world), move(World world, dx, dy, dz), getPosition(), getVelocity()

### com.za.minecraft.entities.Inventory
Назначение: Хранилище предметов игрока.
Функции: getSelectedItemStack(), setStackInSlot(int slot, ItemStack stack), addItem(ItemStack stack)

## Networking (UPDATED)
### com.za.minecraft.network.GameServer / GameClient
Назначение: Мультиплеер на базе Kryonet.
Логика: Поддержка 32-битных ID блоков в пакетах.
Функции: start(), connect(String address), sendBlockUpdate(x, y, z, int type)

## Utilities (UPDATED)
### com.za.minecraft.utils.Identifier
Назначение: Стандарт `namespace:path` для всех ресурсов.

### com.za.minecraft.utils.NumericalRegistry (NEW)
Назначение: Реестр с автоматическим управлением целочисленными ID.

### com.za.minecraft.utils.Direction
Назначение: Стандарт смещений (UP, DOWN, NORTH, SOUTH, EAST, WEST).

### com.za.minecraft.utils.I18n
Назначение: Система локализации (JSON-файлы).
