package com.za.minecraft.world.items;

import com.za.minecraft.utils.Identifier;

public class BlockItem extends Item {
    public BlockItem(int id, String name, String texturePath) {
        super(id, name, texturePath);
        this.weight = 2.5f;
    }

    public BlockItem(int id, Identifier identifier, String translationKey, String texturePath) {
        super(id, identifier, translationKey, texturePath);
        this.weight = 2.5f;
    }

    @Override
    public boolean isBlock() {
        return true;
    }

    @Override
    public ViewmodelTransform getViewmodelTransform() {
        return DEFAULT_TRANSFORM_MARKER;
    }
}
