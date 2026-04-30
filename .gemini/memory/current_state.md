## Текущий статус проекта "Zenith"

## Реализованные фичи

- **Modular Block System (Block Components)**:
  - **Composition over Inheritance**: Блоки переведены на компонентную модель (`Container`, `CraftingSurface`, `Carvable`).
  - **Dynamic Hitboxes**: Реализована поддержка "честных" хитбоксов для предметов на поверхностях через расширение Raycast (`addDynamicBoxes`).
  - **Data-Driven Blocks**: Описание блоков и их поведения полностью вынесено в JSON.
  - **Advanced In-World Crafting**: Новая схема взаимодействия (ПКМ для манипуляций, Shift+ПКМ для сбора) с контекстными подсказками.

- **Modular Rendering Architecture (Zenith v2.0)**:
  - **Modular Pipeline**: Бывший монолит `Renderer.java` разделен на `RenderPipeline`, `RenderContext`, `MeshRegistry`, `ChunkRenderSystem`, `EntityRenderSystem` и `OverlayRenderSystem`.
  - **Global Uniform Buffer (UBO)**: Все глобальные данные (Матрицы, Время, Свет) в едином `std140` буфере.
  - **Zero-Alloc Strategy**: Использование пулов объектов (матрицы, векторы) в рендеринге для минимизации нагрузки на GC.
  - **Resource Scanning Engine**: Унифицированный поиск ресурсов (JSON, текстуры) через `ResourceScanner`.
  - **MultiDraw Batching (Phase 2)**: Мир отрисовывается через `glMultiDrawElementsIndirect`. Количество Draw Calls снижено до 2-4 на кадр.
  - **Mesh Pooling**: Централизованный буфер на 1.5 ГБ. Нулевое переключение VBO/EBO при отрисовке мира.
  - **Occlusion Culling (Phase 1)**: BFS обход с проверкой масок связности секций (Cave Culling).
  - **Robust BFS**: Алгоритм видимости корректно работает в пустоте.

- **Vertex Compression Engine**: Размер вершины 28 байт. Front-to-Back сортировка для Early Z.

- **Entity Lifecycle Refactor (v1.1)**:
  - **onUpdate Lifecycle**: Переход на защищенный метод `onUpdate` для разделения системной и пользовательской логики сущностей.
  - **Rotation Interpolation**: Исправлено дерганье при вращении через `lerpAngle`.

- **Treecapitator & Naturalness System**: Система срубания деревьев и флаг природности блоков.

- **Zenith WorldGen v1.1 (100% Data-Driven)**: Declarative Density, AST Engine, 5D Climate.

## В работе (Next Phase: GPU Optimization)
- **Decoupled Input System**: Вынос логики взаимодействия блоков в специализированные контроллеры компонентов.
- **Level of Detail (LOD) (Phase 3)**: Упрощенная геометрия для дальних дистанций.
- **Zero-Alloc Phase 4**: Переработка системы частиц на пулы объектов.
- **Enhanced Entity AI**: Расширение логики поведения мобов на основе новой системы `onUpdate`.

## Roadmap
- [x] Milestone 3: Моддинг-API и Расширяемость.
- [x] Оптимизация Phase 1 & 2 (Culling + MultiDraw).
- [/] Milestone 4: Полноценный геймплей.
- [ ] Milestone 5: Прогрессия и Металлургия.
