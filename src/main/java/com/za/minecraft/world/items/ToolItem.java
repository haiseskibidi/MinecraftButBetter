package com.za.minecraft.world.items;

public class ToolItem extends Item {
    public enum ToolType {
        KNIFE,
        PICKAXE,
        CROWBAR,
        NONE
    }

    private final ToolType toolType;
    private final float efficiency;
    private final int maxDurability;

    public ToolItem(byte id, String name, String texturePath, ToolType toolType, float efficiency, int maxDurability) {
        super(id, name, texturePath);
        this.toolType = toolType;
        this.efficiency = efficiency;
        this.maxDurability = maxDurability;
    }

    public boolean isEffectiveAgainst(byte blockType) {
        // Простая логика: кирка для камня/руды, лом для металла, нож для листвы
        switch (toolType) {
            case PICKAXE:
                return isStoneBased(blockType);
            case CROWBAR:
                return isMetalBased(blockType);
            case KNIFE:
                return isSoftBased(blockType);
            default:
                return false;
        }
    }

    private boolean isStoneBased(byte type) {
        return type == com.za.minecraft.world.blocks.BlockType.STONE || 
               type == com.za.minecraft.world.blocks.BlockType.COBBLESTONE ||
               type == com.za.minecraft.world.blocks.BlockType.IRON_ORE ||
               type == com.za.minecraft.world.blocks.BlockType.COAL_ORE ||
               type == com.za.minecraft.world.blocks.BlockType.GOLD_ORE ||
               type == com.za.minecraft.world.blocks.BlockType.STONE_BRICKS;
    }

    private boolean isMetalBased(byte type) {
        return type == com.za.minecraft.world.blocks.BlockType.RUSTY_METAL ||
               type == com.za.minecraft.world.blocks.BlockType.ASPHALT; // Асфальт тоже ломом
    }

    private boolean isSoftBased(byte type) {
        return type == com.za.minecraft.world.blocks.BlockType.LEAVES ||
               type == com.za.minecraft.world.blocks.BlockType.GRASS;
    }

    private static final ViewmodelTransform TOOL_TRANSFORM = new ViewmodelTransform(
        0.60f, -0.70f, -0.80f, // Position
        0f, -90.0f, 45.0f,  // Rotation (Minecraft handheld style)
        0.85f                  // Scale
    );

    @Override
    public ViewmodelTransform getViewmodelTransform() {
        return TOOL_TRANSFORM;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public float getEfficiency() {
        return efficiency;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    @Override
    public boolean isTool() {
        return true;
    }

    @Override
    public float getMiningSpeed(byte blockType) {
        float speed = efficiency;
        if (!isEffectiveAgainst(blockType)) {
            speed *= 0.3f; // 3x slower for non-effective tools
        }
        return speed;
    }
}
