# Zenith API: UI, Layouts & Journal

Этот документ содержит полные технические спецификации системы интерфейсов, инвентарей и обучения.

---

## 1. Универсальная UI и Система Инвентаря v4
Система полностью отделяет данные (хранилища) от представления (GUI).

### 1.1. Слой UI и Экранов
- **Screen Interface**: Базовый контракт (`init`, `render`, `handleMouseClick`, `handleKeyPress`, `handleScroll`).
- **ScrollPanel**: Прокрутка с использованием `glScissor` для отсечения контента. OpenGL координаты инвертированы (Y-bottom).
- **UIRenderer**: Фасад рендеринга. Делегирует работу суб-рендерерам: `UIPrimitives`, `SlotRenderer`, `HUDRenderer`, `InventoryScreenRenderer`, `MenuRenderer`.

### 1.2. Слой Данных (Storage)
- **IInventory**: Базовый интерфейс хранилища.
- **ItemInventory**: Реализация инвентаря внутри предмета (`ItemStack`). Позволяет носить рюкзаки в инвентаре.
- **Slot**: Обертка над индексом в `IInventory`. Поддерживает валидацию через `EquipmentComponent`.

---

## 2. Data-Driven Layout System
Интерфейсы верстаются в JSON (`resources/zenith/gui/*.json`).

### 2.1. Ключевые концепции
- **Юниты `"1s"`**: Размер слота + spacing.
- **Относительное позиционирование**: Поля `relativeTo` (якорь), `relativeAlign / relativeAlignY`.
- **Адаптивный фон**: Поле `background.includeGroups` вычисляет Bounding Box вокруг указанных групп.
- **Condition**: Условное отображение групп (например, `"condition": "has_pouch"`).

### 2.2. Управление
- **Quick-Move (Shift+Click)**: Правила перемещения задаются в массиве `quickMoveTo` в JSON.
- **Ghost Icons (Placeholders)**: Текстуры для пустых слотов. Рисуются с `isGrayscale` эффектом.
- **Drag-to-Distribute**: Распределение предметов мазком зажатой кнопки мыши.

---

## 5. HUD Configuration (`gui/hud.json`)
... (существующий текст) ...

---

## 6. Условные группы в GUI (Conditions)
Группы слотов могут появляться динамически в зависимости от стейта.
- **Пример**: `"condition": "has_pouch"` в `player_inventory.json` отрендерит группу только при наличии предмета-сумки.


---

## 3. Система форматирования текста (FontRenderer)
Поддерживает стандартные цветовые коды и динамические анимации через символ `$`.

### 3.1. Коды форматирования
- **Цвета**: `$0-9`, `$a-f` (стандартная палитра Minecraft).
- **Стили**:
  - `$l`: **Жирный (Bold)**.
  - `$o`: *Курсив (Italic)*.
  - `$r`: Сброс всех стилей и цветов.
- **Анимации**:
  - `$z`: **Радуга (Rainbow)** — плавный перелив всех цветов.
  - `$g`: **Свечение (Glow)** — пульсация яркости.
  - `$v`: **Волна (Wave)** — буквы плавают вверх-вниз.
  - `$q`: **Тряска (Shake)** — мелкая дрожь букв (эффект нестабильности).

### 3.2. Markdown Tooltips
Описания предметов в JSON поддерживают упрощенную Markdown-разметку:
- `### Заголовок`: Переводит строку в бирюзовый жирный капс.
- `**текст**`: Делает текст жирным.
- `*текст*`: Делает текст серым курсивом (используется для лора/цитат).
- `- Пункт списка`: Создает список с буллитом `•`. Поддерживает многострочный перенос с идеальным вертикальным выравниванием.

---

## 4. Survivor's Tablet (Дневник)
Модульная система обучения.

### 4.1. Алгоритм добавления статьи:
1. Создать JSON в `journal/entries/`.
2. Добавить имя в `.index` этого каталога.
3. Добавить Identifier в массив `entries` категории в `journal/categories/`.
4. Добавить переводы в `lang/ru_ru.json`.

**Пример статьи:**
```json
{
  "identifier": "zenith:pottery",
  "title": "journal.entry.pottery.title",
  "icon": "zenith:fired_vessel",
  "elements": [
    { "type": "header", "value": "journal.entry.pottery.step1" },
    { "type": "recipe", "value": "zenith:unfired_vessel" },
    { "type": "tip", "value": "journal.entry.pottery.tip" }
  ]
}
```
