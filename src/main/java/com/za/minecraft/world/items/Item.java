package com.za.minecraft.world.items;

public class Item {
    protected final byte id;
    protected final String name;
    protected final String texturePath;

    public Item(byte id, String name, String texturePath) {
        this.id = id;
        this.name = name;
        this.texturePath = texturePath;
    }

    public byte getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTexturePath() {
        return texturePath;
    }
    
    public boolean isTool() {
        return false;
    }

    public boolean isFood() {
        return false;
    }

    public boolean isBlock() {
        return false;
    }
}
