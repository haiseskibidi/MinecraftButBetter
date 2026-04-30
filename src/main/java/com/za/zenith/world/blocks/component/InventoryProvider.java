package com.za.zenith.world.blocks.component;

/**
 * Интерфейс-маркер для компонентов, которым требуется инвентарь в блоке.
 */
public interface InventoryProvider {
    /**
     * @return Требуемое количество слотов инвентаря.
     */
    int getRequiredInventorySize();
}
