package com.za.minecraft.entities.inventory;

import com.za.minecraft.world.inventory.IInventory;
import com.za.minecraft.world.items.ItemStack;
import java.util.function.Predicate;

public class Slot {
    private final IInventory inventory;
    private final int index;
    private final String type;
    private Predicate<ItemStack> validator;

    public Slot(IInventory inventory, int index, String type) {
        this.inventory = inventory;
        this.index = index;
        this.type = type;
        this.validator = (stack) -> true;
    }

    public Slot withValidator(Predicate<ItemStack> validator) {
        this.validator = validator;
        return this;
    }

    public IInventory getInventory() {
        return inventory;
    }

    public int getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public ItemStack getStack() {
        return inventory.getStack(index);
    }

    public void setStack(ItemStack stack) {
        inventory.setStack(index, stack);
    }

    public boolean isItemValid(ItemStack stack) {
        if (stack == null) return true;
        return validator.test(stack) && inventory.isItemValid(index, stack);
    }
}
