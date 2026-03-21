package com.za.minecraft.world.items;

public class ItemStack {
    private final Item item;
    private int count;
    private int durability;

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int count) {
        this.item = item;
        this.count = count;
        if (item instanceof ToolItem) {
            this.durability = ((ToolItem) item).getMaxDurability();
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

    public ItemStack copy() {
        ItemStack copy = new ItemStack(item, count);
        copy.setDurability(durability);
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
        this.count -= toTake;
        return newStack;
    }

    public boolean isStackableWith(ItemStack other) {
        if (other == null) return false;
        if (item.isTool()) return false; // Tools don't stack
        return item.getId() == other.getItem().getId();
    }
}
