package com.za.zenith.world.items;

import com.za.zenith.world.inventory.ItemInventory;

public class ItemStack {
    private final Item item;
    private int count;
    private int durability;
    private com.za.zenith.utils.Identifier rarity;
    private final com.za.zenith.world.items.stats.StatContainer stats = new com.za.zenith.world.items.stats.StatContainer();
    private final java.util.List<com.za.zenith.utils.Identifier> activeAffixes = new java.util.ArrayList<>();
    private com.za.zenith.world.inventory.ItemInventory itemInventory;
    private float temperature;

    public void addAffix(com.za.zenith.utils.Identifier affixId) {
        if (!activeAffixes.contains(affixId)) {
            activeAffixes.add(affixId);
        }
    }

    public java.util.List<com.za.zenith.utils.Identifier> getActiveAffixes() {
        return activeAffixes;
    }

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int count) {
        if (item == null) {
            com.za.zenith.utils.Logger.error("Attempted to create ItemStack with null Item!");
            this.item = Items.HAND; // Fallback to hand to avoid further crashes
            this.count = 0;
            this.rarity = com.za.zenith.world.items.stats.RarityRegistry.COMMON;
            return;
        }
        this.item = item;
        this.count = count;
        this.rarity = item.getDefaultRarity();
        
        // Инициализация прочности из компонентов
        com.za.zenith.world.items.component.ToolComponent toolComp = item.getComponent(com.za.zenith.world.items.component.ToolComponent.class);
        if (toolComp != null) {
            this.durability = toolComp.maxDurability();
        }

        // Инициализация температуры
        com.za.zenith.world.items.component.ThermalComponent thermal = item.getComponent(com.za.zenith.world.items.component.ThermalComponent.class);
        if (thermal != null) {
            this.temperature = thermal.initialTemperature();
        } else {
            this.temperature = 20.0f; // Default ambient
        }

        com.za.zenith.world.items.component.BagComponent bagComp = item.getComponent(com.za.zenith.world.items.component.BagComponent.class);
        if (bagComp != null) {
            this.itemInventory = new com.za.zenith.world.inventory.ItemInventory(bagComp.getSlots());
        }
    }

    public com.za.zenith.utils.Identifier getRarity() {
        return rarity;
    }

    public void setRarity(com.za.zenith.utils.Identifier rarity) {
        this.rarity = rarity;
    }

    public com.za.zenith.world.items.stats.StatContainer getStats() {
        return stats;
    }

    /**
     * Gets the total stat value, combining base item stats, stack modifiers, and affixes.
     */
    public float getStat(com.za.zenith.utils.Identifier statId) {
        float totalValue = item.getStat(statId);
        
        // Add manual stack modifiers
        java.util.Map<com.za.zenith.utils.Identifier, Float> stackStats = stats.getAllStats();
        if (stackStats.containsKey(statId)) {
            totalValue += stackStats.get(statId);
        }

        // Add affix modifiers
        for (com.za.zenith.utils.Identifier affixId : activeAffixes) {
            com.za.zenith.world.items.stats.AffixDefinition affix = com.za.zenith.world.items.stats.AffixRegistry.get(affixId);
            if (affix != null && affix.stats().containsKey(statId)) {
                totalValue += affix.stats().get(statId);
            }
        }
        
        // Clamp result using stat definition
        com.za.zenith.world.items.stats.StatDefinition def = com.za.zenith.world.items.stats.StatRegistry.get(statId);
        if (def != null) {
            totalValue = Math.clamp(totalValue, def.minValue(), def.maxValue());
        }
        
        return totalValue;
    }

    public String getDisplayName() {
        StringBuilder name = new StringBuilder();
        
        // Add Prefixes
        for (com.za.zenith.utils.Identifier affixId : activeAffixes) {
            com.za.zenith.world.items.stats.AffixDefinition affix = com.za.zenith.world.items.stats.AffixRegistry.get(affixId);
            if (affix != null && affix.type() == com.za.zenith.world.items.stats.AffixDefinition.Type.PREFIX) {
                name.append(com.za.zenith.utils.I18n.get(getGenderedTranslationKey(affix.translationKey()))).append(" ");
            }
        }

        // Base Name
        name.append(item.getName());

        // Add Suffixes
        for (com.za.zenith.utils.Identifier affixId : activeAffixes) {
            com.za.zenith.world.items.stats.AffixDefinition affix = com.za.zenith.world.items.stats.AffixRegistry.get(affixId);
            if (affix != null && affix.type() == com.za.zenith.world.items.stats.AffixDefinition.Type.SUFFIX) {
                name.append(" ").append(com.za.zenith.utils.I18n.get(getGenderedTranslationKey(affix.translationKey())));
            }
        }

        return name.toString();
    }

    /**
     * Returns the display name with rarity color and bold formatting codes.
     */
    public String getFullDisplayName() {
        com.za.zenith.world.items.stats.RarityDefinition rarityDef = com.za.zenith.world.items.stats.RarityRegistry.get(this.rarity);
        String rarityColor = (rarityDef != null) ? rarityDef.colorCode() : "$f";
        return rarityColor + "$l" + getDisplayName();
    }

    private String getGenderedTranslationKey(String baseKey) {
        return switch (item.getGender()) {
            case MASCULINE -> baseKey + ".m";
            case FEMININE -> baseKey + ".f";
            case NEUTER -> baseKey + ".n";
        };
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    /**
     * Updates item temperature based on ambient temperature.
     */
    public void updateTemperature(float ambientTemp, float deltaTime) {
        com.za.zenith.world.items.component.ThermalComponent thermal = item.getComponent(com.za.zenith.world.items.component.ThermalComponent.class);
        float k = (thermal != null) ? thermal.specificHeatCapacity() : 0.05f;
        
        // Simple Newton's Law of Cooling
        float delta = ambientTemp - temperature;
        this.temperature += delta * k * deltaTime;
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

    public ItemInventory getItemInventory() {
        return itemInventory;
    }

    public ItemStack copy() {
        ItemStack copy = new ItemStack(item, count);
        copy.setDurability(durability);
        copy.setRarity(rarity);
        for (com.za.zenith.utils.Identifier affixId : activeAffixes) {
            copy.addAffix(affixId);
        }
        if (this.itemInventory != null) {
            copy.itemInventory = this.itemInventory.copy();
        }
        return copy;
    }

    /**
     * Splits the stack by taking 'amount' from it.
     * @param amount The amount to take.
     * @return A new ItemStack with the taken amount, or null if amount <= 0.
     */
    public ItemStack split(int amount) {
        if (amount <= 0) return null;
        int toTake = Math.min(amount, count);
        ItemStack newStack = new ItemStack(item, toTake);
        newStack.setDurability(durability);
        newStack.setRarity(rarity);
        for (com.za.zenith.utils.Identifier affixId : activeAffixes) {
            newStack.addAffix(affixId);
        }
        // Note: Splitting bags should probably not be possible if they have inventory, 
        // but since their maxStackSize is 1, it only copies the reference/data when amount is 1.
        if (this.itemInventory != null && toTake == this.count) {
             newStack.itemInventory = this.itemInventory;
             this.itemInventory = null;
        } else if (this.itemInventory != null) {
             newStack.itemInventory = this.itemInventory.copy();
        }
        this.count -= toTake;
        return newStack;
    }

    public boolean isStackableWith(ItemStack other) {
        if (other == null) return false;
        if (item.getId() != other.getItem().getId()) return false;
        return count < item.getMaxStackSize();
    }

    public boolean isFull() {
        return count >= item.getMaxStackSize();
    }

    public int getAvailableSpace() {
        return Math.max(0, item.getMaxStackSize() - count);
    }
}


