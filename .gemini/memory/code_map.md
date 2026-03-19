# Code Map: MinecraftButBetter

## Core Engine
### com.za.minecraft.Application
Назначение: Точка входа в приложение, обрабатывает аргументы командной строки для выбора режима (Singleplayer, Host, Client).
Экспорты/Функции: main(String[] args)
Зависимости: com.za.minecraft.engine.core.GameLoop

### com.za.minecraft.engine.core.GameLoop
Назначение: Главный игровой цикл, управляет инициализацией, обновлением состояния и рендерингом.
Экспорты/Функции: getInstance(), getPlayer(), runSingleplayer(), runAsHost(String name), runAsClient(String name, String address), isInventoryOpen()
Зависимости: Window, Timer, Camera, InputManager, Renderer, World, Player, Hotbar, GameServer, GameClient, PauseMenu

### com.za.minecraft.engine.core.Window
Назначение: Управление окном GLFW и контекстом OpenGL.
Экспорты/Функции: init(), update(), cleanup(), shouldClose(), getWidth(), getHeight(), getAspectRatio(), isKeyPressed(int keyCode)
Зависимости: GLFW, GL

### com.za.minecraft.engine.core.Timer
Назначение: Учет времени для синхронизации FPS/UPS и вычисления delta time.
Экспорты/Функции: updateDelta(), getDelta(), getDeltaF()
Зависимости: System.nanoTime()

## Graphics
### com.za.minecraft.engine.graphics.Renderer
Назначение: Основной класс отрисовки мира, сущностей и UI.
Экспорты/Функции: init(int width, int height), render(Window window, Camera camera, World world, RaycastResult highlightedBlock, GameClient client), cleanup(), getAtlas(), getUIRenderer()
Зависимости: Mesh, Shader, Camera, World, TextureAtlas, Framebuffer, PostProcessor, UIRenderer, DebugRenderer

### com.za.minecraft.engine.graphics.Camera
Назначение: Управление видом игрока, матрицами проекции и вида.
Экспорты/Функции: updateAspectRatio(float ratio), getViewMatrix(), getProjectionMatrix(), move(Vector3f offset), rotate(float pitch, float yaw)
Зависимости: JOML (Matrix4f, Vector3f)

### com.za.minecraft.engine.graphics.TextureAtlas
Назначение: Управление атласом текстур блоков.
Экспорты/Функции: getUVs(String texturePath), getTexture()
Зависимости: Texture, BlockRegistry

### com.za.minecraft.engine.graphics.ui.UIRenderer
Назначение: Отрисовка элементов интерфейса (инвентарь, хотбар, прицел).
Экспорты/Функции: renderInventory(int width, int height, TextureAtlas atlas), renderHotbar(int width, int height, Player player), renderCrosshair(int width, int height)
Зависимости: Mesh, Shader, Texture

## World & Blocks
### com.za.minecraft.world.World
Назначение: Хранилище чанков и управление состоянием блоков в мире.
Экспорты/Функции: getBlock(int x, int y, int z), setBlock(int x, int y, int z, Block block), getLoadedChunks(), getChunk(ChunkPos pos)
Зависимости: Chunk, TerrainGenerator, ChunkPos, BlockPos

### com.za.minecraft.world.chunks.Chunk
Назначение: Контейнер для блоков размером 16x256x16.
Экспорты/Функции: getBlock(int x, int y, int z), setBlock(int x, int y, int z, Block block), buildMesh(TextureAtlas atlas)
Зависимости: Block, Mesh, ChunkMeshGenerator

### com.za.minecraft.world.blocks.BlockRegistry
Назначение: Регистрация всех типов блоков и их свойств.
Экспорты/Функции: registerBlock(BlockDefinition def), getBlock(byte id), getTextures(byte id), allTextureKeys()
Зависимости: BlockDefinition, BlockType, BlockTextures

### com.za.minecraft.world.blocks.Block
Назначение: Объектное представление блока в мире.
Экспорты/Функции: getType(), setType(byte type)
Зависимости: BlockType

## Entities & Physics
### com.za.minecraft.entities.Player
Назначение: Сущность игрока, управляет позицией, инвентарем и коллизиями.
Экспорты/Функции: update(float delta, World world), getPosition(), getInventory()
Зависимости: Vector3f, Inventory, World, AABB

### com.za.minecraft.world.physics.Raycast
Назначение: Алгоритм пересечения луча с воксельной сеткой (для выбора блоков).
Экспорты/Функции: cast(Vector3f origin, Vector3f direction, float distance, World world)
Зависимости: World, RaycastResult

## Generation
### com.za.minecraft.world.generation.TerrainGenerator
Назначение: Генерация ландшафта и структур.
Экспорты/Функции: generateTerrain(Chunk chunk), generateStructures(World world, Chunk chunk)
Зависимости: PerlinNoise, SimplexNoise, TreeGenerator

## Networking
### com.za.minecraft.network.GameServer
Назначение: Серверная часть для мультиплеера.
Экспорты/Функции: start(), stop(), broadcast(NetworkPacket packet)
Зависимости: NetworkPacket

### com.za.minecraft.network.GameClient
Назначение: Клиентская часть для подключения к серверу.
Экспорты/Функции: connect(String address), disconnect(), sendPlayerPosition()
Зависимости: NetworkPacket, World, Player
