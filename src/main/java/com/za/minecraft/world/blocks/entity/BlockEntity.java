package com.za.minecraft.world.blocks.entity;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;

/**
 * Базовый класс для всех сущностей блоков.
 * Сущности блоков хранят дополнительную информацию (энергию, инвентарь, прогресс),
 * которую нельзя уместить в один байт типа блока.
 */
public abstract class BlockEntity {
    protected final BlockPos pos;
    protected World world;
    protected boolean removed = false;

    public BlockEntity(BlockPos pos) {
        this.pos = pos;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public World getWorld() {
        return world;
    }

    public void setRemoved() {
        this.removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }
}
