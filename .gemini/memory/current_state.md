## Текущий статус проекта "Zenith"

## Реализованные фичи

- **Zenith GPU Optimization Suite (v1.5)**:
  - **MultiDraw Batching (Phase 2)**: Мир отрисовывается через `glMultiDrawElementsIndirect`. Количество Draw Calls снижено с ~1000 до 2-4 на кадр.
  - **Mesh Pooling**: Централизованный буфер на 1.5 ГБ (1ГБ Вершины, 512МБ Индексы). Нулевое переключение VBO/EBO при отрисовке мира.
  - **Occlusion Culling (Phase 1)**: BFS обход с проверкой масок связности секций (Cave Culling). Чанки, не видимые из текущей позиции (например, в пещерах), не отправляются на GPU.
  - **Wrap-around Protection**: Система версионности пула мешей. При заполнении буфера происходит полная очистка и пересборка мешей, исключая порчу данных.
  - **Robust BFS**: Алгоритм видимости теперь корректно работает в пустоте (за пределами загруженных чанков).
  - **Global Uniform Buffer (UBO)**: Все глобальные данные (Матрицы, Время, Свет) в `std140` буфере.

- **Vertex Compression Engine**: Размер вершины 28 байт. Front-to-Back сортировка для Early Z.

- **Treecapitator & Naturalness System**: Система срубания деревьев и флаг природности блоков.

- **Zenith WorldGen v1.1 (100% Data-Driven)**: Declarative Density, AST Engine, 5D Climate.

## В работе (Next Phase: GPU Optimization)
- **Level of Detail (LOD) (Phase 3)**: Упрощенная геометрия для дальних дистанций.
- **Zero-Alloc Phase 4**: Переработка системы частиц на пулы объектов.

## Roadmap
- [x] Milestone 3: Моддинг-API и Расширяемость.
- [x] Оптимизация Phase 1 & 2 (Culling + MultiDraw).
- [/] Milestone 4: Полноценный геймплей.
- [ ] Milestone 5: Прогрессия и Металлургия.
