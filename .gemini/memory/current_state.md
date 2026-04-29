## Текущий статус проекта "Zenith"

## Реализованные фичи

- **Zenith Engine Centralization (v1.1 UPDATED)**:
  - **Global Uniform Buffer (UBO)**: Все глобальные данные (Матрицы Projection/View, Время, Солнце, Амбиент, Цвет травы) перенесены в централизованный `GlobalData` буфер. Это сократило количество вызовов `glUniform` и устранило "размазывание логики" (Logic Smearing) по рендерерам.
  - **Automated Resource Discovery**: Внедрен `ResourceScanner`, который автоматически находит все JSON-ресурсы (Блоки, Предметы, Анимации, Рецепты). Ручное ведение `.index` файлов больше не требуется. Исправлена проблема регистрации анимаций (отсечение расширений).
  - **Unified Shader Pipeline**: Базовый класс `Shader` теперь автоматически привязывает UBO к любому шейдеру, содержащему блок `GlobalData`.
  - **Entity Hierarchy Refactoring**: Базовый класс `Entity` использует Template Method для `update()`, гарантируя сохранение `prevPosition/prevRotation`. Логика движения и физики унифицирована через `onUpdate()`.
  - **GameLoop Cleanup**: Исправлена обработка `elapsedTime`. Теперь дельта времени вычисляется один раз за кадр, предотвращая "замирание" анимаций.

- **Vertex Compression Engine (v1.0 COMPLETED)**:
  - **Bit-Packed Layout**: Размер вершины сокращен до 28 байт. Все данные упакованы в 7 флоатов.
  - **Front-to-Back Sorting**: Внедрена сортировка чанков от игрока для активации Early Z-culling на GPU.
  - **Visual Stability Fixes**: Исправлено дрожание хайлайта блока и трещин.

- **Treecapitator & Naturalness System**:
  - **Felling Mechanics**: Система постепенного срубания деревьев по стадиям.
  - **BIT_NATURAL Validation**: Автоматическая пометка блоков структур флагом "природности".

- **Zenith WorldGen v1.1 (100% Data-Driven)**:
  - **Declarative Density**: Ландшафт полностью в `final_density.json`.
  - **AST Density Engine**: Вычисления через дерево объектов.
  - **5D Climate Space**: Выбор биома на основе климатических параметров.

## В работе (Next Phase: GPU Optimization)
- **Occlusion Culling**: Отсечение полностью закрытых чанков.
- **Level of Detail (LOD)**: Упрощенная геометрия для дальних дистанций.
- **Chunk Batching / MultiDraw**: Объединение чанков в минимальное количество Draw Calls.
- **Zero-Alloc Phase 4**: Переработка системы частиц на пулы объектов.

## Roadmap
- [x] Milestone 2: Базовые механики выживания.
- [x] Milestone 3: Моддинг-API и Расширяемость.
- [x] Интегрированная система настроек и UBO.
- [/] Milestone 4: Полноценный геймплей.
- [ ] Milestone 5: Прогрессия и Металлургия.
