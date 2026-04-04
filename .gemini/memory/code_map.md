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

## UI & Animation Editor (v1.0 NEW)
### com.za.minecraft.engine.graphics.ui.editor.animation.AnimationEditorScreen (NEW)
Назначение: Главный экран Animation Studio.
Функции: 3D вьюпорт, интерактивный таймлайн, выбор частей тела через Raycasting, Grab/Rotate трансформации, экспорт в JSON.

### com.za.minecraft.engine.graphics.ui.Screen (UPDATED)
Назначение: Базовый интерфейс окон.
Функции: Добавлены методы `isScene()` (для изоляции), `handleMouseMove()` и `handleMouseRelease()` (для интерактивности).

### com.za.minecraft.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Основной рендерер интерфейса.
Функции: Метод `renderDeveloperPanel` сделан публичным для использования в редакторе.

## World & Items (UPDATED)
### com.za.minecraft.world.blocks.BlockDefinition (UPDATED)
Назначение: Физические и визуальные свойства блока.
Функции: Хранит `interaction_cooldown` для настройки скорости сбора.

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

### com.za.minecraft.engine.graphics.Renderer (UPDATED)
Назначение: Главный контроллер рендеринга.
Функции: Управляет проходами отрисовки мира, сущностей, Viewmodel и UI. Теперь инициализирует выделенный `viewmodelShader` и обновляет `MiningVFXManager`. Реализована корректная очистка цвета неба при возврате из редактора.

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

