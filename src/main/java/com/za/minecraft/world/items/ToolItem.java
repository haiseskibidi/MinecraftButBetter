package com.za.minecraft.world.items;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.items.component.ToolComponent;

public class ToolItem extends Item {
    public enum ToolType {
        KNIFE,
        PICKAXE,
        CROWBAR,
        NONE
    }

    public ToolItem(int id, String name, String texturePath, ToolType toolType, float efficiency, int maxDurability) {
        super(id, name, texturePath);
        addComponent(ToolComponent.class, new ToolComponent(toolType, efficiency, maxDurability));
    }

    public ToolItem(int id, Identifier identifier, String translationKey, String texturePath, ToolType toolType, float efficiency, int maxDurability) {
        super(id, identifier, translationKey, texturePath);
        addComponent(ToolComponent.class, new ToolComponent(toolType, efficiency, maxDurability));
    }

    public boolean isEffectiveAgainst(int blockType) {
        ToolComponent c = getComponent(ToolComponent.class);
        if (c == null) return false;
        
        if (c.isEffectiveAgainstAll()) return true;
        
        com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(blockType);
        String required = def.getRequiredTool();
        
        return required.equalsIgnoreCase(c.type().name());
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
        ToolComponent c = getComponent(ToolComponent.class);
        return c != null ? c.type() : ToolType.NONE;
    }

    public float getEfficiency() {
        ToolComponent c = getComponent(ToolComponent.class);
        return c != null ? c.efficiency() : 1.0f;
    }

    public int getMaxDurability() {
        ToolComponent c = getComponent(ToolComponent.class);
        return c != null ? c.maxDurability() : 0;
    }

    @Override
    public float getMiningSpeed(int blockType) {
        ToolComponent c = getComponent(ToolComponent.class);
        if (c == null) return super.getMiningSpeed(blockType);

        float speed = c.efficiency();
        if (!isEffectiveAgainst(blockType)) {
            speed *= 0.3f; // 3x slower for non-effective tools
        }
        return speed;
    }
    }

