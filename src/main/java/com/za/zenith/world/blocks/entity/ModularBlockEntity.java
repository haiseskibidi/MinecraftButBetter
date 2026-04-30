package com.za.zenith.world.blocks.entity;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.inventory.IInventory;
import com.za.zenith.world.inventory.SimpleInventory;
import com.za.zenith.engine.graphics.ui.interaction.BlockInfoProvider;
import com.za.zenith.world.blocks.BlockRegistry;
import java.util.HashMap;
import java.util.Map;

/**
 * Универсальная сущность блока, управляемая компонентами.
 * Хранит инвентарь и динамические свойства.
 */
public class ModularBlockEntity extends BlockEntity implements IInventory, ITickable, BlockInfoProvider {
    private SimpleInventory inventory;
    private final Map<String, Float> floatProperties = new HashMap<>();
    private final Map<String, String> stringProperties = new HashMap<>();

    // --- Standard Property Keys ---
    public static final String PROP_CARVE_MASK = "carve_mask";
    public static final String PROP_CRAFT_PROGRESS = "craft_progress";
    public static final String PROP_CRAFT_HITS = "craft_hits";
    public static final String PROP_ENERGY = "energy";
    public static final String PROP_MAX_ENERGY = "max_energy";

    public ModularBlockEntity(BlockPos pos) {
        super(pos);
    }

    /**
     * Автоматически инициализирует инвентарь, если он еще не создан, 
     * опрашивая все компоненты блока о требуемом количестве слотов.
     */
    public void ensureInventory(com.za.zenith.world.blocks.BlockDefinition def) {
        if (inventory != null) return;
        
        int totalSlots = 0;
        for (var comp : def.getComponents()) {
            if (comp instanceof com.za.zenith.world.blocks.component.InventoryProvider provider) {
                totalSlots = Math.max(totalSlots, provider.getRequiredInventorySize());
            }
        }
        
        if (totalSlots > 0) {
            initInventory(totalSlots);
        }
    }

    public void initInventory(int size) {
        if (this.inventory == null || this.inventory.size() != size) {
            this.inventory = new SimpleInventory(size);
        }
    }

    // --- IInventory ---
    @Override
    public ItemStack getStack(int slot) {
        return inventory != null ? inventory.getStack(slot) : null;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (inventory != null) {
            inventory.setStack(slot, stack);
        }
    }

    @Override
    public int size() {
        return inventory != null ? inventory.size() : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inventory != null && inventory.isItemValid(slot, stack);
    }

    @Override
    public boolean isSlotActive(int slot) {
        return inventory != null && inventory.isSlotActive(slot);
    }

    // --- Properties ---
    public float getFloat(String key, float defaultValue) {
        return floatProperties.getOrDefault(key, defaultValue);
    }

    public void setFloat(String key, float value) {
        floatProperties.put(key, value);
    }

    public String getString(String key, String defaultValue) {
        return stringProperties.getOrDefault(key, defaultValue);
    }

    public void setString(String key, String value) {
        stringProperties.put(key, value);
    }

    // --- ITickable ---
    @Override
    public void update(float deltaTime) {
        if (world == null) return;
        var block = world.getBlock(pos);
        if (block == null) return;
        var def = BlockRegistry.getBlock(block.getType());
        if (def == null) return;
        
        for (var component : def.getComponents()) {
            component.onTick(world, pos);
        }
    }

    @Override
    public boolean shouldTick() {
        if (world == null) return false;
        var block = world.getBlock(pos);
        if (block == null) return false;
        var def = BlockRegistry.getBlock(block.getType());
        if (def == null) return false;

        for (var component : def.getComponents()) {
            if (component instanceof com.za.zenith.world.blocks.component.TickableComponent) return true;
        }
        return false;
    }

    // --- BlockInfoProvider ---
    @Override
    public String getDynamicStatus() {
        if (inventory == null) return "empty";
        boolean hasItems = false;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i) != null) {
                hasItems = true;
                break;
            }
        }
        return hasItems ? "has_item" : "empty";
    }

    @Override
    public float getInteractionProgress() {
        return getFloat("craft_progress", -1.0f);
    }

    @Override
    public com.za.zenith.world.items.ItemStack getHeldStack() {
        if (inventory == null) return null;
        // Возвращаем первый попавшийся предмет для проверки правил (например, для обжига)
        for (int i = 0; i < inventory.size(); i++) {
            var s = inventory.getStack(i);
            if (s != null) return s;
        }
        return null;
    }
}
