package com.za.zenith.world.items;

import com.za.zenith.utils.Identifier;

/**
 * Статические ссылки на базовые предметы для удобного доступа из кода.
 */
public class Items {
    public static Item STONE_KNIFE;
    public static Item STONE_AXE;
    public static Item WOODEN_SHOVEL;
    public static Item STONE_SHOVEL;
    public static Item SCRAP_PICKAXE;
    public static Item CROWBAR;
    public static Item FUEL_CANISTER;
    public static Item ADMIN_HAMMER;
    public static Item RAW_MEAT;
    public static Item COOKED_MEAT;
    public static Item CANNED_FOOD;
    public static Item FLINT;
    public static Item STICK;
    public static Item ROCK;
    public static Item STONE_ARROWHEAD;
    public static Item AXE_HEAD;
    public static Item PICKAXE_HEAD;
    public static Item UNFIRED_VESSEL;
    public static Item FIRED_VESSEL;
    public static Item CLAY_BALL;
    public static Item STRAW;
    public static Item SHARPENED_STICK;
    public static Item PLANT_FIBER;
    public static Item STRING;
    public static Item FIRE_STARTER;
    public static Item MAGNETITE;
    public static Item HAND;
    public static Item POUCH;
    public static Item OFFHAND_DUMMY;
    public static Item SCRAP_METAL;
    public static Item COPPER_WIRE;
    public static Item BACKPACK;

    public static void init() {        for (java.lang.reflect.Field field : Items.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    Identifier id = Identifier.of("zenith", field.getName().toLowerCase());
                    Item item = ItemRegistry.getRegistry().get(id);
                    if (item != null) {
                        field.set(null, item);
                    }
                } catch (Exception e) {
                    com.za.zenith.utils.Logger.error("Failed to auto-init item field: " + field.getName());
                }
            }
        }
    }
}
