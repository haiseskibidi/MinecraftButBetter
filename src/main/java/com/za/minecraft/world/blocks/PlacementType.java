package com.za.minecraft.world.blocks;

/**
 * Определяет логику установки и предпросмотра блока.
 */
public enum PlacementType {
    DEFAULT,    // Обычный блок (куб)
    SLAB,       // Полублок (верх/низ/вертикально)
    STAIRS,     // Лестница (поворот по взгляду)
    LOG         // Бревно (ориентация по оси)
}
