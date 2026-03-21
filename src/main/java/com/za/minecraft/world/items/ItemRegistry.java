package com.za.minecraft.world.items;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.utils.NumericalRegistry;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.blocks.Blocks;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private static final NumericalRegistry<Item> REGISTRY = new NumericalRegistry<>();
    
    public static void init() {
        // Map blocks to items automatically after DataLoader has loaded blocks
        BlockRegistry.getRegistry().getIds().forEach(id -> {
            var def = BlockRegistry.getRegistry().get(id);
            int intId = BlockRegistry.getRegistry().getId(id);
            if (intId != 0) { // Not AIR
                Item blockItem = new BlockItem(intId, def.getIdentifier(), def.getName(), def.getTextures() != null ? def.getTextures().getNorth() : "");
                registerItem(blockItem);
            }
        });
    }

    public static void registerItem(Item item) {
        REGISTRY.register(item.getIdentifier(), item.getId(), item);
    }

    public static Item getItem(int id) {
        return REGISTRY.get(id);
    }
    
    public static Item getItem(Identifier id) {
        return REGISTRY.get(id);
    }
    
    public static NumericalRegistry<Item> getRegistry() {
        return REGISTRY;
    }

    public static Map<Integer, Item> getAllItems() {
        Map<Integer, Item> map = new HashMap<>();
        for (Identifier id : REGISTRY.getIds()) {
            map.put(REGISTRY.getId(id), REGISTRY.get(id));
        }
        return map;
    }
}
