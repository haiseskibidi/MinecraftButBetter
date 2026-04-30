package com.za.zenith.engine.graphics.ui.interaction;

import java.util.List;

/**
 * Одиночное правило взаимодействия для конкретного блока.
 */
public record InteractionRule(
    String condition,      // null, "has_item", "is_empty", "is_burning", etc.
    String heldItem,       // ID предмета в руке
    String heldTag,        // Тег предмета в руке
    String blockItemTag,   // Тег предмета внутри блока
    Boolean sneak,         // true = обязательно приседать
    Boolean handEmpty,     // true = рука должна быть пуста
    Boolean handFull,      // true = в руке должен быть любой предмет
    String button,         // "LMB", "RMB"
    Float minY,            // Минимальная локальная высота попадания (0.0 - 1.0)
    String hint,           // Ключ локализации
    boolean showProgress   // Показывать ли прогресс-бар
) {}
