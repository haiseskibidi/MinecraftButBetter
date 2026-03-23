package com.za.minecraft.world.blocks;

public record DropRule(String requiredToolType, String dropItemIdentifier, float chance) {
    /**
     * @param requiredToolType Тип инструмента из ToolType (например, "knife", "axe"). "none" означает любой инструмент или руку.
     * @param dropItemIdentifier ID предмета, который выпадет.
     * @param chance Вероятность выпадения (от 0.0 до 1.0).
     */
}