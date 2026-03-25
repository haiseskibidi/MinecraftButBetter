package com.za.minecraft.world.items;

import com.za.minecraft.utils.I18n;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.items.component.FoodComponent;
import com.za.minecraft.world.items.component.ItemComponent;
import com.za.minecraft.world.items.component.ToolComponent;

import java.util.HashMap;
import java.util.Map;

public class Item {
    protected final int id;
    protected final Identifier identifier;
    protected final String name;
    protected final String texturePath;
    protected float weight = 1.0f;
    protected float visualScale = 1.0f;
    protected float miningSpeed = 0.1f;
    protected int maxStackSize = -1; // -1 means use type default
    
    private final Map<Class<? extends ItemComponent>, ItemComponent> components = new HashMap<>();

    public Item(int id, String name, String texturePath) {
        this.id = id;
        this.identifier = Identifier.of(name.replace("item.", "").replace(".", ":"));
        this.name = name;
        this.texturePath = texturePath;
    }
    
    public Item(int id, Identifier identifier, String translationKey, String texturePath) {
        this.id = id;
        this.identifier = identifier;
        this.name = translationKey;
        this.texturePath = texturePath;
    }

    public <T extends ItemComponent> void addComponent(Class<T> clazz, T component) {
        components.put(clazz, component);
    }

    @SuppressWarnings("unchecked")
    public <T extends ItemComponent> T getComponent(Class<T> clazz) {
        return (T) components.get(clazz);
    }

    public <T extends ItemComponent> boolean hasComponent(Class<T> clazz) {
        return components.containsKey(clazz);
    }

    public int getId() {
        return id;
    }
    
    public Identifier getIdentifier() {
        return identifier;
    }

    public String getName() {
        return I18n.get(name);
    }

    public String getTexturePath() {
        return texturePath;
    }

    public float getWeight() {
        return weight;
    }
    
    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getVisualScale() {
        return visualScale;
    }

    public void setVisualScale(float visualScale) {
        this.visualScale = visualScale;
    }

    public float getBaseMiningSpeed() {
        return miningSpeed;
    }

    public void setMiningSpeed(float miningSpeed) {
        this.miningSpeed = miningSpeed;
    }
    
    public boolean isTool() {
        return hasComponent(ToolComponent.class);
    }

    public boolean isFood() {
        return hasComponent(FoodComponent.class);
    }

    public boolean isBlock() {
        return false;
    }

    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    public int getMaxStackSize() {
        if (maxStackSize != -1) return maxStackSize;
        if (isTool()) return 1;
        if (isBlock()) return 128;
        return 16;
    }

    public static class ViewmodelTransform {
        public float px, py, pz;
        public float rx, ry, rz;
        public float scale;

        public ViewmodelTransform(float px, float py, float pz, float rx, float ry, float rz, float scale) {
            this.px = px; this.py = py; this.pz = pz;
            this.rx = rx; this.ry = ry; this.rz = rz;
            this.scale = scale;
        }
    }

    private static final ViewmodelTransform DEFAULT_TRANSFORM = new ViewmodelTransform(0.55f, -0.65f, -0.75f, 0.0f, 90.0f, 0.0f, 0.85f);


    public ViewmodelTransform getViewmodelTransform() {
        return DEFAULT_TRANSFORM;
    }

    public float getMiningSpeed(int blockType) {
        com.za.minecraft.world.blocks.BlockDefinition blockDef = com.za.minecraft.world.blocks.BlockRegistry.getBlock(blockType);
        if (blockDef == null) return 1.0f;

        String required = blockDef.getRequiredTool();
        
        // Если блок вообще не требует инструментов (например, воздух или простые блоки)
        if (required == null || required.equalsIgnoreCase("none")) {
            return 1.0f;
        }

        ToolComponent tool = getComponent(ToolComponent.class);
        if (tool != null) {
            // Если инструмент эффективен против этого типа блоков (или против всех)
            if (tool.isEffectiveAgainstAll() || tool.type().name().equalsIgnoreCase(required)) {
                return tool.efficiency();
            } else {
                // Если инструмент не подходит, он работает на базовой скорости предмета
                return this.miningSpeed;
            }
        }
        
        // Скорость рукой (или блоком) по блоку, требующему инструмент
        return this.miningSpeed; 
    }
}
