package com.za.minecraft.world.blocks.entity;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.inventory.IInventory;
import com.za.minecraft.world.inventory.SimpleInventory;
import com.za.minecraft.world.items.ItemStack;

/**
 * Block Entity for chests and other generic containers.
 */
public class ChestBlockEntity extends BlockEntity implements IInventory {
    private final SimpleInventory inventory;

    public ChestBlockEntity(BlockPos pos, int size) {
        super(pos);
        this.inventory = new SimpleInventory(size);
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.getStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.setStack(slot, stack);
    }

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inventory.isItemValid(slot, stack);
    }

    @Override
    public boolean isSlotActive(int slot) {
        return inventory.isSlotActive(slot);
    }
}
