package com.za.zenith.world.items;

import com.za.zenith.world.inventory.ItemInventory;

public class ItemStack {
    private final Item item;
    private int count;
    private int durability;
    private ItemInventory itemInventory;
    private float temperature;

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int count) {
        this.item = item;
        this.count = count;
        
        // Инициализация прочности из компонентов
        com.za.zenith.world.items.component.ToolComponent toolComp = item.getComponent(com.za.zenith.world.items.component.ToolComponent.class);
        if (toolComp != null) {
            this.durability = toolComp.maxDurability();
        }

        // Инициализация температуры
        com.za.zenith.world.items.component.ThermalComponent thermal = item.getComponent(com.za.zenith.world.items.component.ThermalComponent.class);
        if (thermal != null) {
            this.temperature = thermal.initialTemperature();
        } else {
            this.temperature = 20.0f; // Default ambient
        }

        com.za.zenith.world.items.component.BagComponent bagComp = item.getComponent(com.za.zenith.world.items.component.BagComponent.class);
        if (bagComp != null) {
            this.itemInventory = new ItemInventory(bagComp.getSlots());
        }
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    /**
     * Updates item temperature based on ambient temperature.
     */
    public void updateTemperature(float ambientTemp, float deltaTime) {
        com.za.zenith.world.items.component.ThermalComponent thermal = item.getComponent(com.za.zenith.world.items.component.ThermalComponent.class);
        float k = (thermal != null) ? thermal.specificHeatCapacity() : 0.05f;
        
        // Simple Newton's Law of Cooling
        float delta = ambientTemp - temperature;
        this.temperature += delta * k * deltaTime;
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
