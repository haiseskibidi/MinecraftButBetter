package com.za.zenith.entities;

import com.za.zenith.utils.Identifier;
import org.joml.Vector3f;

/**
 * Определение типа сущности из JSON.
 * Хранит визуальные и физические параметры.
 */
public record EntityDefinition(
    Identifier identifier,
    String modelType, // "item", "block", "custom"
    String texture,   // Путь к текстуре или ID блока
    Vector3f visualScale,
    Vector3f hitboxSize
) {}


