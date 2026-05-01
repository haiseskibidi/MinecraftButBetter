package com.za.zenith.world.items;

import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.NumericalRegistry;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.Blocks;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private static final NumericalRegistry<Item> REGISTRY = new NumericalRegistry<>();
    
    public static void init() {
        // Map blocks to items automatically after DataLoader has loaded blocks
        BlockRegistry.getRegistry().getIds().forEach(id -> {
            var def = BlockRegistry.getRegistry().get(id);
            int intId = BlockRegistry.getRegistry().getId(id);
            
            Item blockItem = new BlockItem(intId, def.getIdentifier(), def.getName(), def.getTextures() != null ? def.getTextures().getNorth() : "");
            blockItem.setInteractionCooldown(def.getInteractionCooldown());
            
            // Assign a relaxed/flat grip for blocks so fingers don't clip weirdly
            com.za.zenith.engine.graphics.model.GripDefinition blockGrip = new com.za.zenith.engine.graphics.model.GripDefinition(
                new float[]{0, 15, 0}, null,
                new float[]{0, 15, 0}, null,
                new float[]{0, 20, 0}, null
            );
            blockItem.addComponent(com.za.zenith.world.items.component.ViewmodelComponent.class, 
                new com.za.zenith.world.items.component.ViewmodelComponent(null, new float[]{-3.2f, 0.0f, -3.2f}, new float[]{0, 45, 0}, 0.4f, blockGrip));
                
            registerItem(blockItem);
        });
    }

    public static void registerItem(Item item) {
        Item oldItem = REGISTRY.get(item.getIdentifier());
        REGISTRY.register(item.getIdentifier(), item.getId(), item);
        
        // --- HOT RELOAD SYNC ---
        // If we are replacing an existing item, we must update all ItemStacks 
        // currently held by the player/world to point to the new instance.
        if (oldItem != null && oldItem != item) {
            com.za.zenith.engine.core.GameLoop game = com.za.zenith.engine.core.GameLoop.getInstance();
            if (game != null && game.getPlayer() != null) {
                com.za.zenith.entities.Player player = game.getPlayer();
                if (player.getInventory() != null) {
                    for (ItemStack stack : player.getInventory().getAllStacks()) {
                        if (stack != null && stack.getItem() == oldItem) {
                            // Use reflection to update the private 'item' field in ItemStack
                            try {
                                java.lang.reflect.Field itemField = ItemStack.class.getDeclaredField("item");
                                itemField.setAccessible(true);
                                itemField.set(stack, item);
                            } catch (Exception e) {
                                com.za.zenith.utils.Logger.error("Failed to sync ItemStack during hot-reload: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
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


