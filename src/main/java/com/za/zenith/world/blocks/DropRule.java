package com.za.zenith.world.blocks;

public record DropRule(String requiredToolType, String dropItemIdentifier, float chance, boolean dropOnHit, float durabilityPenalty) {
    /**
     * @param requiredToolType Тип инструмента из ToolType (например, "knife", "axe"). "none" означает любой инструмент или руку.
     * @param dropItemIdentifier ID предмета, который выпадет.
     * @param chance Вероятность выпадения (от 0.0 до 1.0).
     * @param dropOnHit Если true, предмет может выпасть при каждом ударе по блоку (например, ломиком), а не только при разрушении.
     * @param durabilityPenalty Коэффициент урона блоку при выпадении (умножается на maxHealth * chance). По умолчанию 0.6.
     */
    
    // Вспомогательный конструктор для обратной совместимости
    public DropRule(String requiredToolType, String dropItemIdentifier, float chance, boolean dropOnHit) {
        this(requiredToolType, dropItemIdentifier, chance, dropOnHit, 0.6f);
    }
}
