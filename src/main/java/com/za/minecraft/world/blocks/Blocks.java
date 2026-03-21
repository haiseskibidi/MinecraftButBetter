package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;

/**
 * Статические ссылки на базовые блоки для удобного доступа из кода.
 * Инициализируются в DataLoader.
 */
public class Blocks {
    public static BlockDefinition AIR;
    public static BlockDefinition GRASS_BLOCK;
    public static BlockDefinition DIRT;
    public static BlockDefinition STONE;
    public static BlockDefinition OAK_LOG;
    public static BlockDefinition OAK_LEAVES;
    public static BlockDefinition OAK_PLANKS;
    public static BlockDefinition COBBLESTONE;
    public static BlockDefinition BEDROCK;
    public static BlockDefinition SAND;
    public static BlockDefinition GRAVEL;
    public static BlockDefinition GOLD_ORE;
    public static BlockDefinition IRON_ORE;
    public static BlockDefinition COAL_ORE;
    public static BlockDefinition BOOKSHELF;
    public static BlockDefinition MOSSY_COBBLESTONE;
    public static BlockDefinition OBSIDIAN;
    public static BlockDefinition ASPHALT;
    public static BlockDefinition RUSTY_METAL;
    public static BlockDefinition GLASS;
    public static BlockDefinition BRICKS;
    public static BlockDefinition STONE_BRICKS;
    public static BlockDefinition CYAN_CONCRETE;
    public static BlockDefinition GRAY_CONCRETE;
    public static BlockDefinition WHITE_CONCRETE;
    public static BlockDefinition STONE_SLAB;
    public static BlockDefinition STONE_STAIRS;
    public static BlockDefinition BRICK_SLAB;
    public static BlockDefinition BRICK_STAIRS;
    public static BlockDefinition CAMPFIRE;
    public static BlockDefinition GENERATOR;
    public static BlockDefinition CABLE;
    public static BlockDefinition ELECTRIC_LAMP;
    public static BlockDefinition BATTERY;

    public static void init() {
        for (java.lang.reflect.Field field : Blocks.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    Identifier id = Identifier.of("minecraft", field.getName().toLowerCase());
                    BlockDefinition def = BlockRegistry.getRegistry().get(id);
                    if (def != null) {
                        field.set(null, def);
                    }
                } catch (Exception e) {
                    com.za.minecraft.utils.Logger.error("Failed to auto-init block field: " + field.getName());
                }
            }
        }
    }
}
