package com.za.minecraft.world.items;

public class BlockItem extends Item {
    public BlockItem(byte id, String name, String texturePath) {
        super(id, name, texturePath);
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
