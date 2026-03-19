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
Функции: getInstance(), runSingleplayer(), runAsHost(String name), runAsClient(String name, String address), init(), loop(), input(), update(float interval), render(), cleanup()
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

## Graphics
### com.za.minecraft.engine.graphics.Renderer
Назначение: Координация всех процессов отрисовки (мир, превью блоков, UI).
Функции: init(int width, int height), render(Window window, Camera camera, World world, RaycastResult highlightedBlock, GameClient client), renderDebug(float fps, int width, int height), setPreviewBlock(BlockPos pos, Block block)
Зависимости: Shader, Mesh, TextureAtlas, Framebuffer, PostProcessor, UIRenderer, DebugRenderer

### com.za.minecraft.engine.graphics.Camera
Назначение: Управление вектором взгляда игрока и матрицами проекции.
Функции: getViewMatrix(), getProjectionMatrix(), moveRotation(float rx, float ry, float rz), updateAspectRatio(float ratio)

### com.za.minecraft.engine.graphics.ui.UIRenderer
Назначение: Отрисовка 2D элементов (прицел, хотбар, инвентарь, меню паузы).
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
### com.za.minecraft.world.World
Назначение: Управление чанками и глобальное хранилище блоков.
Функции: getBlock(int x, int y, int z), setBlock(int x, int y, int z, Block block), getLoadedChunks()
Зависимости: Chunk, TerrainGenerator, ChunkPos

### com.za.minecraft.world.chunks.Chunk
Назначение: Контейнер для блоков 16x256x16.
Функции: getBlock(int x, int y, int z), setBlock(int x, int y, int z, Block block), buildMesh(TextureAtlas atlas), setNeedsMeshUpdate(boolean)
Зависимости: Block, Mesh, ChunkMeshGenerator

### com.za.minecraft.world.blocks.BlockRegistry
Назначение: Центральный реестр определений блоков.
Функции: registerBlock(BlockDefinition def), getBlock(byte id), getTextures(byte id)
Зависимости: BlockDefinition, BlockType

### com.za.minecraft.world.blocks.Block
Назначение: Экземпляр блока с типом и метаданными (направление).
Функции: getType(), getMetadata(), setType(byte type), setMetadata(byte meta)

## Items System (NEW)
### com.za.minecraft.world.items.Item
Назначение: Базовый класс для всех предметов.
Поля: id, name, texturePath

### com.za.minecraft.world.items.BlockItem (NEW)
Назначение: Предметы, представляющие блоки. Используется для разделения логики рендеринга иконок.

### com.za.minecraft.world.items.ToolItem
Назначение: Инструменты с параметрами эффективности и прочности.
Функции: getToolType(), getEfficiency(), getMaxDurability()

### com.za.minecraft.world.items.FoodItem (NEW)
Назначение: Съедобные предметы.
Параметры: nutrition, saturationBonus.

### com.za.minecraft.world.items.ItemStack
Назначение: Контейнер для предмета в инвентаре (хранит количество и текущую прочность).
Функции: getItem(), getCount(), getDurability()

### com.za.minecraft.world.items.ItemRegistry
Назначение: Реестр всех предметов и автоматический маппинг блоков в предметы.
Функции: registerItem(Item item), getItem(byte id), getAllItems()

## Entities & Physics
### com.za.minecraft.entities.Entity (NEW)
Назначение: Базовый физический объект в мире. 
Функции: update(float delta, World world), move(World world, float dx, float dy, float dz), getPosition(), getVelocity(), getRotation(), getBoundingBox(), isOnGround(), setFlying(boolean)
Зависимости: Vector3f, AABB, World

### com.za.minecraft.entities.LivingEntity (NEW)
Назначение: Сущность со здоровьем (Игрок, Мобы). Наследует Entity.
Функции: takeDamage(float amount), heal(float amount), isDead()

### com.za.minecraft.entities.Player
Назначение: Сущность игрока под управлением человека. Наследует LivingEntity. 
Функции: update(float delta, World world), jump(), addNoise(float), setContinuousNoise(float), eat(FoodItem), getNoiseLevel(), getHunger()
Зависимости: Vector3f, Inventory, LivingEntity

### com.za.minecraft.entities.ScoutEntity (NEW)
Назначение: Зараженный-скаут. ИИ логика слуха и погони. Наследует LivingEntity.
Функции: update(float delta, World world), getCurrentState()
Зависимости: AIState, Player, World

### com.za.minecraft.entities.ai.AIState (NEW)
Назначение: Перечисление состояний ИИ (IDLE, WANDER, SEARCH, CHASE, ATTACK).

### com.za.minecraft.entities.Inventory
Назначение: Хранилище предметов игрока (хотбар).
Функции: getSelectedItemStack(), setStackInSlot(int slot, ItemStack stack), nextSlot(), previousSlot()
Зависимости: ItemStack, ItemRegistry

### com.za.minecraft.world.physics.Raycast
Назначение: Алгоритм прослеживания луча для выбора блоков.
Функции: raycast(World world, Vector3f origin, Vector3f direction)
Зависимости: RaycastResult, World

## Networking
### com.za.minecraft.network.GameServer / GameClient
Назначение: Реализация мультиплеера на базе Kryonet.
Функции: start(), connect(String address), sendBlockUpdate(int x, int y, int z, byte type), sendPlayerPosition()
Зависимости: NetworkPacket, Kryo
