package com.za.zenith.world.blocks.entity;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.utils.Identifier;

/**
 * Сущность блока для пня (Stump).
 * Хранит предмет, лежащий на пне, и прогресс его обработки.
 */
public class StumpBlockEntity extends BlockEntity implements ICraftingSurface, ITickable {
    private ItemStack[] inventory = new ItemStack[9];
    private int progress = 0;
    private Identifier currentToolId = null;
    private int carvingMask = 0xFFFF; // Default to fully carved for existing stumps
    private float carvingCooldown = 0.0f;

    public StumpBlockEntity(BlockPos pos) {
        super(pos);
    }

    @Override
    public void update(float deltaTime) {
        if (carvingCooldown > 0) carvingCooldown -= deltaTime;
    }

    @Override
    public boolean shouldTick() {
        return carvingCooldown > 0;
    }

    public boolean canCarve() {
        return carvingCooldown <= 0;
    }

    public int getCarvingMask() {
        return carvingMask;
    }

    public void setCarvingMask(int mask) {
        this.carvingMask = mask;
    }

    public void setCarvingBit(int bitIndex) {
        if (bitIndex >= 0 && bitIndex < 16) {
            this.carvingMask |= (1 << bitIndex);
            this.carvingCooldown = 0.12f; // Кулдаун 120мс
            if (world != null) world.registerTickable(this);
        }
    }

    public boolean isFullyCarved() {
        return (carvingMask & 0xFFFF) == 0xFFFF;
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


