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
    Boolean sneak,         // true = обязательно приседать, false = нельзя, null = неважно
    String button,         // "LMB", "RMB" (по умолчанию RMB)
    String hint,           // Ключ локализации подсказки
    boolean showProgress   // Показывать ли прогресс-бар
) {}
