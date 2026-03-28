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

## UI System (UPDATED)
### com.za.minecraft.engine.graphics.ui.UIAnimationManager (NEW)
Назначение: Менеджер состояний анимации UI элементов.
Функции: Рассчитывает плавный прогресс наведения (hover) для слотов.

### com.za.minecraft.engine.graphics.ui.UIEffectsRenderer (NEW)
Назначение: Специализированный статический рендерер для визуальных эффектов (уголки, рамки).

### com.za.minecraft.engine.graphics.ui.InventoryBlockRenderer (UPDATED)
Назначение: Специализированный рендерер для 3D моделей блоков в GUI.
Функции: Отрисовка блоков с поддержкой ротации.

### com.za.minecraft.engine.graphics.ui.UIRenderer (UPDATED)
Назначение: Отрисовка 2D элементов (прицел, хотбар, инвентарь, меню паузы).
Логика: Реализует SDF-октагоны для слотов и кинетическую анимацию предметов. Скрывает хотбар в HUD при открытых экранах.

### com.za.minecraft.engine.graphics.ui.GUIConfig (UPDATED)
Назначение: Конфиг GUI с поддержкой `hudVisible` и `SelectionStyle`.

## Graphics (UPDATED)
### src/main/resources/shaders/ui_fragment.glsl (UPDATED)
Назначение: Пиксельный шейдер для UI.
Логика: Поддержка SDF-форм (октагонов) и эффектов Grayscale.

### src/main/resources/shaders/inventory_block_fragment.glsl (UPDATED)
Назначение: Шейдер для 3D иконок. Реализует 3-точечное освещение.

### com.za.minecraft.world.chunks.ChunkMeshGenerator (UPDATED)
Назначение: Генератор мешей. Исправлен тинтинг листвы в инвентаре.

## Core Engine
### com.za.minecraft.engine.core.GameLoop (UPDATED)
Назначение: Главный цикл. Добавлен геттер для `Timer`.
