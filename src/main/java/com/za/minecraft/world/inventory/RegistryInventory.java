package com.za.minecraft.world.inventory;

import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * A virtual inventory that displays all registered items.
 * Useful for Creative/Developer mode panels.
 */
public class RegistryInventory implements IInventory {
    private final List<Item> items;

    public RegistryInventory() {
        this.items = new ArrayList<>(ItemRegistry.getAllItems().values());
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < items.size()) {
            Item item = items.get(slot);
            return new ItemStack(item, item.isBlock() ? 64 : 1);
        }
        return null;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        // Read-only inventory for the registry view
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false; // Cannot put items INTO the registry
    }

    @Override
    public boolean isSlotActive(int slot) {
        return true;
    }
}
