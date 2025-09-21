# Правила разработки для AI: Клон Minecraft

## Основные принципы архитектуры

### 1. Модульность превыше всего
- **ВСЕГДА** разделяй функциональность по модулям
- **НЕ СМЕШИВАЙ** логику рендеринга с игровой логикой
- **КАЖДЫЙ КЛАСС** должен иметь одну четко определенную ответственность
- **ЗАВИСИМОСТИ** должны быть явными и минимальными

```java
// ПРАВИЛЬНО: четкое разделение ответственности
public class ChunkRenderer {
    public void render(Chunk chunk, Camera camera) { /* только рендеринг */ }
}

public class ChunkManager {
    public void updateChunk(int x, int z) { /* только управление чанками */ }
}

// НЕПРАВИЛЬНО: смешанная ответственность
public class ChunkRenderer {
    public void render(Chunk chunk, Camera camera) {
        // рендеринг
        updateChunkLogic(); // НЕТ! Не здесь!
        generateTerrain();  // НЕТ! Не здесь!
    }
}
```

### 2. Слоистая архитектура
```
┌─────────────────┐
│   Game Layer    │ ← Игровая логика, состояния
├─────────────────┤
│   World Layer   │ ← Блоки, чанки, физика
├─────────────────┤
│  Engine Layer   │ ← Рендеринг, ввод, звук
├─────────────────┤
│   Core Layer    │ ← Математика, утилиты
└─────────────────┘
```

**ПРАВИЛО:** Верхние слои могут использовать нижние, но НЕ НАОБОРОТ

### 3. Система компонентов
- **ПРЕДПОЧИТАЙ** композицию наследованию
- **ИСПОЛЬЗУЙ** интерфейсы для определения поведения
- **СОЗДАВАЙ** переиспользуемые компоненты

```java
// ПРАВИЛЬНО: композиция
public class Player {
    private final Transform transform;
    private final PhysicsComponent physics;
    private final InventoryComponent inventory;
    private final InputComponent input;
}

// НЕПРАВИЛЬНО: глубокое наследование
public class Player extends Entity extends GameObject extends Transform {
    // слишком связанный код
}
```

## Правила написания кода

### 1. Именование классов и методов
```java
// Классы: PascalCase, существительные
public class ChunkManager { }
public class BlockRenderer { }
public class PhysicsEngine { }

// Методы: camelCase, глаголы
public void updateChunk() { }
public boolean canPlace() { }
public Block getBlock() { }

// Константы: UPPER_SNAKE_CASE
public static final int CHUNK_SIZE = 16;
public static final float GRAVITY = 9.81f;

// Переменные: camelCase, описательные
private List<Chunk> loadedChunks;
private BlockPos playerPosition;
```

### 2. Обработка ошибок
```java
// ПРАВИЛЬНО: явная обработка ошибок
public Optional<Chunk> loadChunk(int x, int z) {
    try {
        Chunk chunk = chunkLoader.load(x, z);
        return Optional.of(chunk);
    } catch (IOException e) {
        Logger.error("Failed to load chunk at {}, {}: {}", x, z, e.getMessage());
        return Optional.empty();
    }
}

// НЕПРАВИЛЬНО: проглатывание ошибок
public Chunk loadChunk(int x, int z) {
    try {
        return chunkLoader.load(x, z);
    } catch (Exception e) {
        return null; // НЕТ! Потеря информации об ошибке
    }
}
```

### 3. Управление ресурсами
```java
// ПРАВИЛЬНО: try-with-resources
public Texture loadTexture(String path) {
    try (InputStream stream = Files.newInputStream(Paths.get(path))) {
        return TextureLoader.load(stream);
    } catch (IOException e) {
        throw new ResourceLoadException("Cannot load texture: " + path, e);
    }
}

// ВСЕГДА освобождай OpenGL ресурсы
public void cleanup() {
    if (textureId != 0) {
        glDeleteTextures(textureId);
        textureId = 0;
    }
}
```

## Правила добавления новых фич

### 1. Новые блоки
```java
// Шаблон для нового блока
public class NewBlock extends Block {
    public NewBlock() {
        super(BlockType.NEW_BLOCK);
    }
    
    @Override
    public AABB getBoundingBox(BlockPos pos) {
        // Определи физические границы
    }
    
    @Override
    public boolean isTransparent() {
        // Пропускает ли свет
    }
    
    @Override
    public void onPlayerInteract(Player player, BlockPos pos, Face face) {
        // Логика взаимодействия
    }
    
    // Регистрация в BlockRegistry
    static {
        BlockRegistry.register(BlockType.NEW_BLOCK, NewBlock::new);
    }
}
```

### 2. Новые системы
```java
// Шаблон для новой системы
public class NewSystem {
    private final List<Component> components = new ArrayList<>();
    private boolean enabled = true;
    
    public void update(float deltaTime) {
        if (!enabled) return;
        
        for (Component component : components) {
            component.update(deltaTime);
        }
    }
    
    public void addComponent(Component component) {
        components.add(component);
    }
    
    public void removeComponent(Component component) {
        components.remove(component);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

### 3. Новые UI элементы
```java
// Базовый интерфейс для UI
public interface UIElement {
    void render(float deltaTime);
    void handleInput(InputEvent event);
    boolean isVisible();
    void setVisible(boolean visible);
}

// Реализация конкретного элемента
public class InventoryUI implements UIElement {
    private boolean visible = false;
    
    @Override
    public void render(float deltaTime) {
        if (!visible) return;
        // Рендеринг инвентаря
    }
    
    @Override
    public void handleInput(InputEvent event) {
        if (!visible) return;
        // Обработка ввода
    }
}
```

## Правила оптимизации

### 1. Не оптимизируй преждевременно
```java
// СНАЧАЛА сделай работающую версию
public List<Block> getVisibleBlocks(Camera camera) {
    List<Block> visible = new ArrayList<>();
    for (Block block : allBlocks) {
        if (camera.canSee(block)) {
            visible.add(block);
        }
    }
    return visible;
}

// ПОТОМ оптимизируй, если нужно
public List<Block> getVisibleBlocks(Camera camera) {
    return allBlocks.parallelStream()
        .filter(camera::canSee)
        .collect(Collectors.toList());
}
```

### 2. Измеряй производительность
```java
public class PerformanceProfiler {
    private static final Map<String, Long> timers = new HashMap<>();
    
    public static void startTimer(String name) {
        timers.put(name, System.nanoTime());
    }
    
    public static void endTimer(String name) {
        long elapsed = System.nanoTime() - timers.get(name);
        Logger.debug("{} took {} ms", name, elapsed / 1_000_000.0);
    }
}
```

### 3. Кешируй дорогие операции
```java
public class ChunkMeshCache {
    private final Map<ChunkPos, ChunkMesh> cache = new ConcurrentHashMap<>();
    
    public ChunkMesh getMesh(Chunk chunk) {
        return cache.computeIfAbsent(chunk.getPos(), pos -> {
            Logger.debug("Generating mesh for chunk {}", pos);
            return MeshGenerator.generate(chunk);
        });
    }
    
    public void invalidate(ChunkPos pos) {
        ChunkMesh old = cache.remove(pos);
        if (old != null) {
            old.cleanup(); // Освобождаем ресурсы
        }
    }
}
```

## Правила отладки и логирования

### 1. Структурированное логирование
```java
public class Logger {
    public static void debug(String message, Object... args) {
        if (Config.DEBUG_MODE) {
            System.out.printf("[DEBUG] " + message + "%n", args);
        }
    }
    
    public static void info(String message, Object... args) {
        System.out.printf("[INFO] " + message + "%n", args);
    }
    
    public static void error(String message, Object... args) {
        System.err.printf("[ERROR] " + message + "%n", args);
    }
    
    public static void error(String message, Throwable t, Object... args) {
        error(message, args);
        t.printStackTrace();
    }
}
```

### 2. Debug рендеринг
```java
public class DebugRenderer {
    private static boolean wireframeMode = false;
    private static boolean showChunkBorders = false;
    private static boolean showCollisionBoxes = false;
    
    public static void renderDebugInfo(World world, Camera camera) {
        if (!Config.DEBUG_MODE) return;
        
        if (showChunkBorders) {
            renderChunkBorders(world, camera);
        }
        
        if (showCollisionBoxes) {
            renderCollisionBoxes(world, camera);
        }
        
        renderPerformanceStats();
    }
    
    public static void toggleWireframe() {
        wireframeMode = !wireframeMode;
        glPolygonMode(GL_FRONT_AND_BACK, wireframeMode ? GL_LINE : GL_FILL);
    }
}
```

## Правила управления состоянием

### 1. Неизменяемые объекты где возможно
```java
// ПРАВИЛЬНО: неизменяемый BlockPos
public record BlockPos(int x, int y, int z) {
    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }
}

// НЕПРАВИЛЬНО: изменяемый BlockPos
public class BlockPos {
    public int x, y, z; // Может привести к багам
    
    public void offset(int dx, int dy, int dz) {
        x += dx; y += dy; z += dz; // Неожиданные изменения
    }
}
```

### 2. Паттерн Builder для сложных объектов
```java
public class Chunk {
    private final ChunkPos position;
    private final Block[][][] blocks;
    private final BiomeType biome;
    
    private Chunk(Builder builder) {
        this.position = builder.position;
        this.blocks = builder.blocks;
        this.biome = builder.biome;
    }
    
    public static class Builder {
        private ChunkPos position;
        private Block[][][] blocks = new Block[16][384][16];
        private BiomeType biome = BiomeType.PLAINS;
        
        public Builder at(ChunkPos position) {
            this.position = position;
            return this;
        }
        
        public Builder withBiome(BiomeType biome) {
            this.biome = biome;
            return this;
        }
        
        public Builder setBlock(int x, int y, int z, Block block) {
            blocks[x][y][z] = block;
            return this;
        }
        
        public Chunk build() {
            Objects.requireNonNull(position, "Position must be set");
            return new Chunk(this);
        }
    }
}
```

## Правила тестирования

### 1. Юнит тесты для логики
```java
@Test
public void testBlockPlacement() {
    // Arrange
    World world = new World();
    BlockPos pos = new BlockPos(0, 1, 0);
    Block block = new StoneBlock();
    
    // Act
    boolean placed = world.setBlock(pos, block);
    
    // Assert
    assertTrue(placed);
    assertEquals(block, world.getBlock(pos));
}
```

### 2. Интеграционные тесты
```java
@Test
public void testChunkGeneration() {
    WorldGenerator generator = new WorldGenerator(12345); // Фиксированный seed
    Chunk chunk = generator.generateChunk(0, 0);
    
    assertNotNull(chunk);
    assertEquals(new ChunkPos(0, 0), chunk.getPosition());
    
    // Проверяем что в чанке есть блоки
    boolean hasBlocks = false;
    for (int x = 0; x < 16; x++) {
        for (int z = 0; z < 16; z++) {
            if (chunk.getBlock(x, 0, z) != Blocks.AIR) {
                hasBlocks = true;
                break;
            }
        }
    }
    assertTrue("Chunk should contain non-air blocks", hasBlocks);
}
```

## Правила рефакторинга

### 1. Правило бойскаута
**ВСЕГДА** оставляй код чище, чем нашел

### 2. Небольшие итерации
- Меняй **ОДНУ** вещь за раз
- Компилируй и тестируй после каждого изменения
- Делай коммиты часто

### 3. Удаляй мертвый код
```java
// НЕ ОСТАВЛЯЙ закомментированный код
public void update() {
    updatePhysics();
    // updateOldPhysics(); // TODO: remove this
    // updateLegacySystem();
    render();
}

// УДАЛЯЙ неиспользуемые методы
// private void oldMethod() { } // УБЕРИ ЭТО
```

## Правила работы с внешними библиотеками

### 1. Изолируй зависимости
```java
// ПРАВИЛЬНО: обертка над LWJGL
public class TextureManager {
    public Texture loadTexture(String path) {
        // Внутри использует LWJGL, но интерфейс свой
    }
}

// НЕПРАВИЛЬНО: прямое использование везде
public class Renderer {
    public void render() {
        glBindTexture(GL_TEXTURE_2D, textureId); // LWJGL везде
    }
}
```

### 2. Создавай адаптеры
```java
public interface AudioEngine {
    void playSound(String name, float volume, float pitch);
    void stopSound(String name);
}

public class OpenALAudioEngine implements AudioEngine {
    // Реализация через OpenAL
}

public class JavaSoundAudioEngine implements AudioEngine {
    // Альтернативная реализация
}
```

## Антипаттерны - чего НЕ делать

### 1. Божественный объект
```java
// НЕТ! Слишком много ответственности
public class Game {
    public void update() { /* физика */ }
    public void render() { /* рендеринг */ }
    public void handleInput() { /* ввод */ }
    public void generateWorld() { /* генерация */ }
    public void playSound() { /* звук */ }
    // ... еще 50 методов
}
```

### 2. Магические числа
```java
// НЕТ!
if (distance > 64) { // Что такое 64?
    unloadChunk();
}

// ДА!
private static final int CHUNK_UNLOAD_DISTANCE = 64;
if (distance > CHUNK_UNLOAD_DISTANCE) {
    unloadChunk();
}
```

### 3. Цепочки instanceof
```java
// НЕТ!
public void handleBlock(Block block) {
    if (block instanceof StoneBlock) {
        // ...
    } else if (block instanceof WoodBlock) {
        // ...
    } else if (block instanceof GrassBlock) {
        // ...
    }
}

// ДА! Полиморфизм
public abstract class Block {
    public abstract void onPlayerInteract(Player player);
}
```

## Чек-лист перед коммитом

- [ ] Код компилируется без ошибок
- [ ] Все тесты проходят
- [ ] Нет hardcoded значений
- [ ] Методы не длиннее 20 строк
- [ ] Классы не больше 200 строк
- [ ] Нет дублирования кода
- [ ] Используются осмысленные имена
- [ ] Добавлена обработка ошибок
- [ ] Освобождаются ресурсы
- [ ] Обновлена документация (если нужно)

## Стиль коммитов (Git)

- Пиши сообщения на английском, в настоящем времени и повелительном наклонении: "Add", "Fix", "Update", а не "Added/Fixed/Updates".
- Формат сообщения:

```
type(scope)!: subject

body (optional)

footer (optional)
```

- **type**:
  - feat: новая функциональность
  - fix: исправление багов
  - refactor: рефакторинг без изменения поведения
  - perf: улучшение производительности
  - test: тесты
  - docs: документация
  - style: правки стиля (formatting, no-op)
  - build: сборка, зависимости
  - ci: CI/CD
  - chore: прочее, технические задачи
  - revert: откат коммита

- **scope** (пример): engine, rendering, world, chunks, blocks, physics, input, entities, ui, audio, build, ci, docs, test
- **subject**: ≤ 50 символов, без точки на конце, кратко и по делу
- **body**: по желанию; объясняет "что" и особенно "почему". Перенос строк ~72 колонки
- **footer**: ссылки на задачи и breaking changes: `Closes #123`, `Refs #456`, `BREAKING CHANGE:`

Примеры:

```text
feat(world): implement greedy meshing for chunk meshes

Reduce triangles by ~35% on average chunks. Behind config flag
to simplify rollout and allow A/B testing.
```

```text
fix(physics): resolve jitter when sliding along vertical slabs

Clamp penetration correction to delta time and separate axis resolution
to avoid oscillation near corners.
```

```text
refactor(blocks): extract BlockRegistry into factory with suppliers

Prepare for data-driven block definitions and lazy initialization.
```

```text
perf(rendering): batch chunk draw calls using instancing

Cuts draw calls by ~60% on 16x16 view. No visual changes.
```

```text
docs(rules): add git commit style guidelines
```

```text
revert: revert "perf(rendering): batch chunk draw calls using instancing"

This reverts commit abcdef1 due to crashes on GPUs without instancing support.
```

## Принципы SOLID

### S - Single Responsibility
Каждый класс должен иметь только одну причину для изменения

### O - Open/Closed
Открыт для расширения, закрыт для модификации

### L - Liskov Substitution
Подклассы должны заменяться базовыми классами

### I - Interface Segregation
Много специфичных интерфейсов лучше одного общего

### D - Dependency Inversion
Зависеть от абстракций, а не от конкретных реализаций

---

**ПОМНИ:** Эти правила созданы для того, чтобы код был читаемым, расширяемым и поддерживаемым. Следуй им, и проект будет развиваться легко и без боли!
