package com.za.minecraft.world.items;

import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.blocks.BlockType;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private static final Map<Byte, Item> ITEMS = new HashMap<>();
    private static final Map<Byte, Item> BLOCK_ITEMS = new HashMap<>();
    public static final byte CROWBAR = 103;

    // Food (201-255 range)
    public static final byte RAW_MEAT = (byte) 201;
    public static final byte COOKED_MEAT = (byte) 202;
    public static final byte CANNED_FOOD = (byte) 203;

    static {
        // Register default "Hand" for empty slot
        ITEMS.put(BlockType.AIR, new Item(BlockType.AIR, "Hand", ""));

        // Register Tools
        registerItem(new ToolItem(ItemType.STONE_KNIFE, "Stone Knife", "minecraft/textures/item/flint.png", ToolItem.ToolType.KNIFE, 1.5f, 50));
        registerItem(new ToolItem(ItemType.SCRAP_PICKAXE, "Scrap Pickaxe", "minecraft/textures/item/wooden_pickaxe.png", ToolItem.ToolType.PICKAXE, 2.0f, 100));
        registerItem(new ToolItem(ItemType.CROWBAR, "Crowbar", "minecraft/textures/block/lever.png", ToolItem.ToolType.CROWBAR, 3.0f, 200));
        registerItem(new Item(ItemType.FUEL_CANISTER, "Fuel Canister", "minecraft/textures/item/honey_bottle.png"));
        registerItem(new ToolItem(ItemType.ADMIN_HAMMER, "Admin Hammer", "minecraft/textures/item/nether_star.png", ToolItem.ToolType.PICKAXE, 1000.0f, 9999) {
            @Override
            public boolean isEffectiveAgainst(byte blockType) {
                return true; // Effective against everything
            }
            @Override
            public float getMiningSpeed(byte blockType) {
                return 1000.0f; // Instant break
            }
        });

        // Register Food
        registerItem(new FoodItem(RAW_MEAT, "Raw Meat", "minecraft/textures/item/beef.png", 2.0f, 1.0f));
        registerItem(new FoodItem(COOKED_MEAT, "Cooked Meat", "minecraft/textures/item/cooked_beef.png", 6.0f, 4.0f));
        registerItem(new FoodItem(CANNED_FOOD, "Canned Food", "minecraft/textures/item/mushroom_stew.png", 4.0f, 8.0f));

        // Map blocks to items automatically
        BlockRegistry.getRegisteredBlocks().forEach((id, def) -> {
            if (id != BlockType.AIR) {
                // Пытаемся найти текстуру предмета, если нет - берем текстуру блока
                String texture = "minecraft/textures/item/" + def.getName().toLowerCase().replace(" ", "_") + ".png";
                // Используем BlockItem для блоков
                Item blockItem = new BlockItem(id, def.getName(), def.getTextures() != null ? def.getTextures().getNorth() : "");
                BLOCK_ITEMS.put(id, blockItem);
            }
        });
    }

    public static void registerItem(Item item) {
        ITEMS.put(item.getId(), item);
    }

    public static Item getItem(byte id) {
        if (ITEMS.containsKey(id)) return ITEMS.get(id);
        return BLOCK_ITEMS.get(id);
    }
    
    public static Map<Byte, Item> getAllItems() {
        Map<Byte, Item> all = new HashMap<>(ITEMS);
        all.putAll(BLOCK_ITEMS);
        return all;
    }
}
