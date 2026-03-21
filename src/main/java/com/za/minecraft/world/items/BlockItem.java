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

    private static final ViewmodelTransform BLOCK_TRANSFORM = new ViewmodelTransform(
        0.55f, -0.50f, -0.90f, // Position
        (float)Math.toDegrees(0.3f), (float)Math.toDegrees(0.6f), (float)Math.toDegrees(0.1f), // Rotation
        0.35f                   // Scale
    );

    @Override
    public ViewmodelTransform getViewmodelTransform() {
        return BLOCK_TRANSFORM;
    }
}
