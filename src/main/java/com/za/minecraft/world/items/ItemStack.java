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
}
