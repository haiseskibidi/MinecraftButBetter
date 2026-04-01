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

## Animation & Locomotion System (v4.5 UPDATED)
### com.za.minecraft.entities.Player (UPDATED)
Назначение: Основная сущность игрока с AAA-системой локомоции.
Функции:
- `update()`: Обновляет физические таймеры локомоции на частоте 170Hz.
- `updateAnimations()`: Визуальный проход. Рассчитывает оффсеты, применяет импульсы приземления и эффекты падения.

## UI System (v4.2 UPDATED)
### com.za.minecraft.engine.graphics.ui.InventoryLayout (UPDATED)
Назначение: Универсальный двигатель верстки.
Функции: Рекурсивный расчет позиций групп, поддержка юнитов `"s"` (слоты), автоматическое центрирование сложных блоков (`centerCombined`) и расчет границ для адаптивных фонов.

### com.za.minecraft.engine.graphics.ui.GUIConfig (UPDATED)
Назначение: POJO-конфигурация GUI. Добавлена поддержка корневого объекта `background` и гибридных типов координат (`Object`).

### com.za.minecraft.engine.graphics.ui.GroupUI (NEW)
Назначение: Контейнер вычисленных экранных координат и размеров для группы слотов.

### com.za.minecraft.engine.graphics.ui.LayoutResult (NEW)
Назначение: Результирующий набор данных верстки (списки слотов, групп и общий фон).

### com.za.minecraft.engine.graphics.ui.PlayerInventoryScreen (UPDATED)
Назначение: Экран инвентаря игрока. Реализует реактивность через `layoutKey`.

### com.za.minecraft.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Отрисовка 2D элементов. Добавлен метод `renderGroupBackground` для рендеринга адаптивных подложек.

## Graphics (UPDATED)
### com.za.minecraft.engine.graphics.Renderer (UPDATED)
Назначение: Главный рендерер игрового мира.
Функции:
- `render()`: Основной проход отрисовки. Добавлен вызов `renderBreakingProxyBlock()`.
- `setBreakingBlock()`: Принимает данные о ломаемом блоке и запускает генерацию `breakingMesh`.
- `renderBreakingProxyBlock()`: Отрисовывает анимированный прокси-меш блока поверх оригинального.

### src/main/resources/shaders/vertex.glsl (UPDATED)
Назначение: Вершинный шейдер. Добавлена логика `uIsProxy` для деформации блоков при ударе.

### src/main/resources/shaders/ui_fragment.glsl (UPDATED)
Назначение: Пиксельный шейдер для UI.
Логика: Поддержка SDF-форм (октагонов) и эффектов Grayscale.

### src/main/resources/shaders/inventory_block_fragment.glsl (UPDATED)
Назначение: Шейдер для 3D иконок. Реализует 3-точечное освещение.

### com.za.minecraft.world.chunks.ChunkMeshGenerator (UPDATED)
Назначение: Генератор мешей. Исправлен тинтинг листвы в инвентаре.

## Core Engine
### com.za.minecraft.engine.input.InputManager (UPDATED)
Назначение: Обработка ввода и игровая логика.
Функции:
- `input()`: Реализует дискретные удары по блокам и рейкаст.
- Управляет кулдаунами `hitCooldownTimer` и сохранением прогресса ломания.

### com.za.minecraft.engine.core.GameLoop (UPDATED)
Назначение: Главный цикл. Добавлен геттер для `Timer`.

## Physical Viewmodel System (v5.0 NEW)
### com.za.minecraft.engine.graphics.Mesh (UPDATED)
Назначение: Низкоуровневая обертка над VBO/VAO.
Функции: Отрисовка, очистка ресурсов. Добавлены методы `getMin()` и `getMax()` для вычисления Bounding Box меша в реальном времени.

### com.za.minecraft.engine.graphics.model.ViewmodelRenderer (UPDATED)
Назначение: Рендерер рук игрока.
Функции: Рендерит иерархию костей. Делегирует отрисовку удерживаемых предметов классу `HeldItemRenderer`.

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
Функции: Применяет AnimationProfile к костям скелета, поддерживает наслоение (blending) нескольких анимаций.

### com.za.minecraft.engine.graphics.model.ViewmodelMeshGenerator
Назначение: Генератор воксельных мешей для костей.
Функции: Создает оптимизированную геометрию на основе кубов, описанных в JSON.

### com.za.minecraft.engine.graphics.model.TwoBoneIK
Назначение: Алгоритм инверсной кинематики для рук.
Функции: Рассчитывает изгиб локтя и плеча для достижения целевой точки кистью.

### com.za.minecraft.engine.graphics.model.ModelRegistry
Назначение: Реестр скелетных моделей.
Функции: Загружает и хранит ViewmodelDefinition из ресурсов.

## Inventory System (NEW)
### com.za.minecraft.world.inventory.ItemInventory
Назначение: Реализация `IInventory` для предметов-контейнеров (рюкзаки, мешочки).
Функции: Позволяет предмету (`ItemStack`) хранить внутри себя другие предметы, поддерживает динамический размер из `BagComponent`, запрещает вложенность рюкзаков.
