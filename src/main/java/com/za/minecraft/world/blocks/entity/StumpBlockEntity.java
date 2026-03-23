package com.za.minecraft.world.blocks.entity;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.utils.Identifier;

/**
 * Сущность блока для пня (Stump).
 * Хранит предмет, лежащий на пне, и прогресс его обработки.
 */
public class StumpBlockEntity extends BlockEntity implements ICraftingSurface {
    private ItemStack[] inventory = new ItemStack[9];
    private int progress = 0;
    private Identifier currentToolId = null;

    public StumpBlockEntity(BlockPos pos) {
        super(pos);
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= 9) return null;
        return inventory[slot];
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 9) {
            inventory[slot] = stack;
            this.progress = 0;
            this.currentToolId = null;
        }
    }

    @Override
    public int getCraftingProgress() {
        return progress;
    }

    @Override
    public void incrementProgress() {
        this.progress++;
    }

    @Override
    public void resetProgress() {
        this.progress = 0;
        this.currentToolId = null;
    }

    public void incrementProgress(Identifier toolId) {
        // Если инструмент сменился в процессе, сбрасываем прогресс для реалистичности
        if (currentToolId != null && toolId != null && !currentToolId.equals(toolId)) {
            progress = 0;
        }
        this.currentToolId = toolId;
        this.progress++;
    }

    public boolean hasItem() {
        for (ItemStack s : inventory) {
            if (s != null) return true;
        }
        return false;
    }

    public ItemStack getHeldStack() {
        // Для обратной совместимости или быстрого доступа к первому предмету
        for (ItemStack s : inventory) {
            if (s != null) return s;
        }
        return null;
    }
}
