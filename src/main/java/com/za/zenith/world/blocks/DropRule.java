package com.za.zenith.world.blocks;

public record DropRule(String requiredToolType, String dropItemIdentifier, float chance, boolean dropOnHit) {
    /**
     * @param requiredToolType Тип инструмента из ToolType (например, "knife", "axe"). "none" означает любой инструмент или руку.
     * @param dropItemIdentifier ID предмета, который выпадет.
     * @param chance Вероятность выпадения (от 0.0 до 1.0).
     * @param dropOnHit Если true, предмет может выпасть при каждом ударе по блоку (например, ломиком), а не только при разрушении.
     */
}
