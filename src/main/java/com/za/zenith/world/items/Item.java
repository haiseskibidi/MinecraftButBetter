package com.za.zenith.world.items;

import com.za.zenith.utils.I18n;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.component.FoodComponent;
import com.za.zenith.world.items.component.ItemComponent;
import com.za.zenith.world.items.component.ToolComponent;

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
    protected float interactionCooldown = -1.0f; 
    protected com.za.zenith.utils.Identifier defaultRarity = com.za.zenith.world.items.stats.RarityRegistry.COMMON;
    protected final com.za.zenith.world.items.stats.StatContainer baseStats = new com.za.zenith.world.items.stats.StatContainer();
    { baseStats.setUseDefaultValues(false); }
    protected Gender gender = Gender.MASCULINE;
    protected final java.util.Set<Identifier> tags = new java.util.HashSet<>();

    public enum Gender {
        MASCULINE, FEMININE, NEUTER
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void addTag(Identifier tag) {
        tags.add(tag);
    }

    public boolean hasTag(Identifier tag) {
        return tags.contains(tag);
    }
    
    public boolean hasTag(String tagStr) {
        return tags.contains(Identifier.of(tagStr));
    }

    public com.za.zenith.utils.Identifier getDefaultRarity() {
        return defaultRarity;
    }

    public void setDefaultRarity(com.za.zenith.utils.Identifier rarity) {
        this.defaultRarity = rarity;
    }

    public com.za.zenith.world.items.stats.StatContainer getBaseStats() {
        return baseStats;
    }

    public float getStat(com.za.zenith.utils.Identifier id) {
        return baseStats.get(id);
    }

    public float getInteractionCooldown() {
        if (interactionCooldown < 0) {
            return com.za.zenith.world.physics.PhysicsSettings.getInstance().baseMiningCooldown;
        }
        return interactionCooldown;
    }

    public void setInteractionCooldown(float interactionCooldown) {
        this.interactionCooldown = interactionCooldown;
    }
    
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

    /**
     * Returns the name of the animation profile to use for this item.
     * @param baseKey The base animation name (e.g. "item_walk", "item_swing").
     * @return The specific animation profile name to use.
     */
    public String getAnimation(String baseKey) {
        // 1. Check for explicit overrides in AnimationComponent
        com.za.zenith.world.items.component.AnimationComponent animComp = getComponent(com.za.zenith.world.items.component.AnimationComponent.class);
        if (animComp != null) {
            String override = animComp.getOverride(baseKey);
            if (override != null) return override;
        }

        // 2. Logic for tools: try <tool_type>_<suffix> (e.g. axe_swing)
        ToolComponent tool = getComponent(ToolComponent.class);
        if (tool != null && baseKey.startsWith("item_")) {
            String typeName = tool.type().name().toLowerCase();
            String suffix = baseKey.substring(5); // skip "item_"
            String custom = typeName + "_" + suffix;
            if (com.za.zenith.entities.parkour.animation.AnimationRegistry.exists(custom)) {
                return custom;
            }
        }

        // 3. Logic for blocks: try block_<suffix>
        if (isBlock() && baseKey.startsWith("item_")) {
            String suffix = baseKey.substring(5);
            String custom = "block_" + suffix;
            if (com.za.zenith.entities.parkour.animation.AnimationRegistry.exists(custom)) {
                return custom;
            }
        }

        return baseKey;
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

    public static final ViewmodelTransform DEFAULT_TRANSFORM_MARKER = new ViewmodelTransform(0,0,0,0,0,0,1);

    private ViewmodelTransform customTransform = null;

    public void setViewmodelTransform(ViewmodelTransform transform) {
        this.customTransform = transform;
    }

    public ViewmodelTransform getViewmodelTransform() {
        if (customTransform != null) return customTransform;
        return DEFAULT_TRANSFORM_MARKER;
    }

    public float getMiningSpeed(int blockType) {
        com.za.zenith.world.blocks.BlockDefinition blockDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(blockType);
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


