package com.za.zenith.engine.graphics.ui.interaction;

/**
 * Интерфейс для динамического предоставления информации от блоков.
 * Реализуется в BlockEntity (например, PitKiln, Stump).
 */
public interface BlockInfoProvider {
    /**
     * @return Текст со статусом (например, "Обжиг..." или "Пусто")
     */
    String getDynamicStatus();

    /**
     * @return Прогресс взаимодействия (0.0 - 1.0). Вернуть -1, если прогресс не нужен.
     */
    float getInteractionProgress();

    /**
     * @return Предмет, находящийся в блоке (для проверки условий подсказок)
     */
    default com.za.zenith.world.items.ItemStack getHeldStack() { return null; }
}
