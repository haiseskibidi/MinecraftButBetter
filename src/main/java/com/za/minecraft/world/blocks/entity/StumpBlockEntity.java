package com.za.minecraft.world.blocks.entity;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.utils.Identifier;

/**
 * Сущность блока для пня (Stump).
 * Хранит предмет, лежащий на пне, и прогресс его обработки.
 */
public class StumpBlockEntity extends BlockEntity {
    private ItemStack heldStack = null;
    private int progress = 0;
    private Identifier currentToolId = null;

    public StumpBlockEntity(BlockPos pos) {
        super(pos);
    }

    public void setHeldStack(ItemStack stack) {
        this.heldStack = stack;
        this.progress = 0;
        this.currentToolId = null;
        
        // Уведомляем мир об изменении, чтобы вызвать перерисовку (если нужно)
        // В нашем случае Renderer опрашивает BE каждый кадр, так что достаточно просто обновить данные.
    }

    public ItemStack getHeldStack() {
        return heldStack;
    }

    public int getProgress() {
        return progress;
    }

    public void incrementProgress(Identifier toolId) {
        // Если инструмент сменился в процессе, сбрасываем прогресс для реалистичности
        if (currentToolId != null && toolId != null && !currentToolId.equals(toolId)) {
            progress = 0;
        }
        this.currentToolId = toolId;
        this.progress++;
    }

    public void resetProgress() {
        this.progress = 0;
        this.currentToolId = null;
    }

    public boolean hasItem() {
        return heldStack != null;
    }
}
