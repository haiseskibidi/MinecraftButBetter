package com.za.minecraft.world.items;

public class BlockItem extends Item {
    public BlockItem(byte id, String name, String texturePath) {
        super(id, name, texturePath);
    }

    @Override
    public boolean isBlock() {
        return true;
    }
}
