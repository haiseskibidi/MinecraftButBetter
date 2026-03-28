package com.za.minecraft.world.items;

import com.za.minecraft.world.inventory.ItemInventory;

public class ItemStack {
    private final Item item;
    private int count;
    private int durability;
    private ItemInventory itemInventory;

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int count) {
        this.item = item;
        this.count = count;
        
        // Инициализация прочности из компонентов (для новых предметов Tier 1)
        com.za.minecraft.world.items.component.ToolComponent toolComp = item.getComponent(com.za.minecraft.world.items.component.ToolComponent.class);
        if (toolComp != null) {
            this.durability = toolComp.maxDurability();
        }

        com.za.minecraft.world.items.component.BagComponent bagComp = item.getComponent(com.za.minecraft.world.items.component.BagComponent.class);
        if (bagComp != null) {
            this.itemInventory = new ItemInventory(bagComp.getSlots());
        }
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public ItemInventory getItemInventory() {
        return itemInventory;
    }

    public ItemStack copy() {
        ItemStack copy = new ItemStack(item, count);
        copy.setDurability(durability);
        if (this.itemInventory != null) {
            copy.itemInventory = this.itemInventory.copy();
        }
        return copy;
    }

    /**
     * Splits the stack by taking 'amount' from it.
     * @param amount The amount to take.
     * @return A new ItemStack with the taken amount, or null if amount <= 0.
     */
    public ItemStack split(int amount) {
        if (amount <= 0) return null;
        int toTake = Math.min(amount, count);
        ItemStack newStack = new ItemStack(item, toTake);
        newStack.setDurability(durability);
        // Note: Splitting bags should probably not be possible if they have inventory, 
        // but since their maxStackSize is 1, it only copies the reference/data when amount is 1.
        if (this.itemInventory != null && toTake == this.count) {
             newStack.itemInventory = this.itemInventory;
        } else if (this.itemInventory != null) {
             newStack.itemInventory = this.itemInventory.copy();
        }
        this.count -= toTake;
        return newStack;
    }

    public boolean isStackableWith(ItemStack other) {
        if (other == null) return false;
        if (item.getId() != other.getItem().getId()) return false;
        return count < item.getMaxStackSize();
    }

    public boolean isFull() {
        return count >= item.getMaxStackSize();
    }

    public int getAvailableSpace() {
        return Math.max(0, item.getMaxStackSize() - count);
    }
}
