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

### com.za.minecraft.engine.input.InputManager
Назначение: Обработка ввода и управление взаимодействием.
Функции: input(), handleInventoryClick(window, button), getSlotAt(), dropStack(), getHoveredSlotIndex(), getDraggedSlots(), clearHeldStack()
Зависимости: Window, Camera, Player, World

## Graphics
### com.za.minecraft.engine.graphics.Renderer
Назначение: Координация всех процессов отрисовки (мир, превью блоков, View Model, UI).
Функции: init(int width, int height), render(Window window, Camera camera, World world, RaycastResult highlightedBlock, GameClient client), renderViewModel(Window, Camera, Player), renderDebug(float fps, int width, int height), setPreviewBlock(BlockPos pos, Block block)
Зависимости: Shader, Mesh, TextureAtlas, Framebuffer, PostProcessor, UIRenderer, DebugRenderer

### com.za.minecraft.engine.graphics.Camera
Назначение: Управление вектором взгляда игрока и матрицами проекции.
Функции: getViewMatrix(), getProjectionMatrix(), moveRotation(float rx, float ry, float rz), updateAspectRatio(float ratio), setOffsets(float x, float y, float z)

### com.za.minecraft.engine.graphics.ui.UIRenderer
Назначение: Отрисовка 2D элементов (прицел, хотбар, инвентарь, меню паузы).
Функции: init(), renderCrosshair(int sw, int sh), renderHotbar(int sw, int sh, DynamicTextureAtlas atlas), renderInventory(int sw, int sh, DynamicTextureAtlas atlas), renderItemIcon(Item item, int x, int y, float size, int sw, int sh, DynamicTextureAtlas atlas)
Зависимости: Shader, Texture, FontRenderer, ItemRegistry, BlockTextureMapper

### com.za.minecraft.engine.graphics.ui.NappingGUI (NEW)
Назначение: Интерфейс 5x5 для механики скалывания камней (Napping).
Функции: render(), handleClick(mouseX, mouseY, sw, sh, Player)
Зависимости: RecipeRegistry, Player, Shader

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

### com.za.minecraft.world.blocks.GeneratorBlockDefinition (NEW)
Назначение: Специальное определение для генератора, позволяющее создавать BlockEntity.
Функции: createBlockEntity(BlockPos pos)

### com.za.minecraft.world.blocks.CableBlockDefinition (NEW)
Назначение: Определение блока кабеля.

### com.za.minecraft.world.blocks.LampBlockDefinition (NEW)
Назначение: Определение блока электрической лампы.

### com.za.minecraft.world.blocks.BatteryBlockDefinition (NEW)
Назначение: Определение блока аккумулятора.

### com.za.minecraft.world.blocks.entity.BlockEntity (NEW)
Назначение: Базовый класс для блоков с состоянием (энергия, инвентарь).
Функции: setRemoved(), isRemoved(), getPos()

### com.za.minecraft.world.blocks.entity.IEnergyStorage (NEW)
Назначение: Стандарт для всех энергозависимых блоков.
Методы: receiveEnergy, extractEnergy, getEnergyStored

### com.za.minecraft.world.blocks.entity.GeneratorBlockEntity (UPDATED)
Назначение: Логика бензогенератора. Реализует IEnergyStorage.

### com.za.minecraft.world.blocks.entity.CableBlockEntity (NEW)
Назначение: Передача энергии между соседними хранилищами.

### com.za.minecraft.world.blocks.entity.LampBlockEntity (NEW)
Назначение: Потребитель энергии (свет).

### com.za.minecraft.world.blocks.entity.BatteryBlockEntity (NEW)
Назначение: Хранилище большого объема энергии. Реализует IEnergyStorage.

### com.za.minecraft.world.blocks.entity.ITickable (NEW)
Назначение: Интерфейс для обновления сущностей блоков каждый тик.

### com.za.minecraft.world.blocks.Block
Назначение: Экземпляр блока с типом и метаданными (направление).
Функции: getType(), getMetadata(), setType(byte type), setMetadata(byte meta)

## Items System (UPDATED)
### com.za.minecraft.world.items.Item
Назначение: Базовый класс для всех предметов.
Поля: id, name, texturePath, weight (NEW)
Функции: getWeight(), setWeight(float)

### com.za.minecraft.world.items.BlockItem (UPDATED)
Назначение: Предметы, представляющие блоки. Имеют повышенный вес (2.5f).

### com.za.minecraft.world.items.ItemMeshGenerator (NEW)
Назначение: Процедурная генерация 3D моделей из 2D текстур (с толщиной) для рендеринга предметов в мире и в руке.

### com.za.minecraft.world.items.ToolItem
Назначение: Инструменты с параметрами эффективности и прочности.
Функции: getToolType(), getEfficiency(), getMaxDurability()

### com.za.minecraft.world.items.FoodItem (NEW)
Назначение: Съедобные предметы.
Параметры: nutrition, saturationBonus.

### com.za.minecraft.world.items.ItemStack
Назначение: Контейнер для предмета в инвентаре (хранит количество и текущую прочность).
Функции: getItem(), getCount(), getDurability(), copy(), split(int amount), isStackableWith(ItemStack)

### com.za.minecraft.world.items.ItemRegistry
Назначение: Реестр всех предметов и автоматический маппинг блоков в предметы.
Функции: registerItem(Item item), getItem(byte id), getAllItems()

## Recipes System (NEW)
### com.za.minecraft.world.recipes.IRecipe
Назначение: Базовый интерфейс для всех рецептов.
Функции: matches(Inventory), getResult(), getType()

### com.za.minecraft.world.recipes.RecipeRegistry
Назначение: Центральное хранилище всех рецептов.
Функции: registerRecipe(IRecipe), getRecipes(String)

### com.za.minecraft.world.recipes.NappingRecipe
Назначение: Рецепт для скалывания камней (Napping).
Поля: inputType, result, pattern

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
Функции: update(float delta, World world), jump(), swing(), getWalkBobTimer(), getSwingProgress(), addNoise(float), setContinuousNoise(float), eat(FoodItem), getNoiseLevel(), getHunger()
Зависимости: Vector3f, Inventory, LivingEntity

### com.za.minecraft.entities.ItemEntity (NEW)
Назначение: Сущность выброшенного предмета в мире. Поддерживает физику, вращение и подбор игроком.
Функции: update(float delta, World world), canBePickedUp(), getStack()

### com.za.minecraft.entities.ScoutEntity (NEW)
Назначение: Зараженный-скаут. ИИ логика слуха и погони. Наследует LivingEntity.
Функции: update(float delta, World world), getCurrentState()
Зависимости: AIState, Player, World

### com.za.minecraft.entities.ai.AIState (NEW)
Назначение: Перечисление состояний ИИ (IDLE, WANDER, SEARCH, CHASE, ATTACK).

### com.za.minecraft.entities.Inventory
Назначение: Хранилище предметов игрока (хотбар + основной инвентарь).
Функции: getSelectedItemStack(), setStackInSlot(int slot, ItemStack stack), quickMove(int slot), sortMainInventory(), addItem(ItemStack stack), nextSlot(), previousSlot()
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

## Utilities
### com.za.minecraft.utils.Direction (NEW)
Назначение: Стандарт смещений для поиска соседних блоков (UP, DOWN, NORTH, SOUTH, EAST, WEST).
Функции: offset(BlockPos), getDx(), getDy(), getDz()

### com.za.minecraft.utils.I18n (NEW)
Назначение: Система локализации на базе JSON-файлов.
Функции: loadLanguage(String langCode), get(String key), format(String key, Object... args)
Зависимости: Gson
