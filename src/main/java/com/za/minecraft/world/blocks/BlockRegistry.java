package com.za.minecraft.world.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class BlockRegistry {
    private static final Map<Byte, BlockDefinition> BLOCKS = new HashMap<>();

    static {
        String base = "minecraft/textures/block/";
        
        registerBlock(new BlockDefinition(BlockType.AIR, "air", false, false));
        registerBlock(new BlockDefinition(BlockType.GRASS, "grass", true, false).setTextures(new BlockTextures(base + "grass_block_top.png", base + "dirt.png", base + "grass_block_side.png")));
        registerBlock(new BlockDefinition(BlockType.DIRT, "dirt", true, false).setTextures(new BlockTextures(base + "dirt.png")));
        registerBlock(new BlockDefinition(BlockType.STONE, "stone", true, false).setTextures(new BlockTextures(base + "stone.png")));
        registerBlock(new BlockDefinition(BlockType.WOOD, "wood", true, false).setTextures(new BlockTextures(base + "oak_log_top.png", base + "oak_log_top.png", base + "oak_log.png")));
        registerBlock(new BlockDefinition(BlockType.LEAVES, "leaves", true, true).setTextures(new BlockTextures(base + "oak_leaves.png")));
        registerBlock(new BlockDefinition(BlockType.OAK_PLANKS, "oak_planks", true, false).setTextures(new BlockTextures(base + "oak_planks.png")));
        registerBlock(new BlockDefinition(BlockType.COBBLESTONE, "cobblestone", true, false).setTextures(new BlockTextures(base + "cobblestone.png")));
        registerBlock(new BlockDefinition(BlockType.BEDROCK, "bedrock", true, false).setTextures(new BlockTextures(base + "bedrock.png")));
        registerBlock(new BlockDefinition(BlockType.SAND, "sand", true, false).setTextures(new BlockTextures(base + "sand.png")));
        registerBlock(new BlockDefinition(BlockType.GRAVEL, "gravel", true, false).setTextures(new BlockTextures(base + "gravel.png")));
        registerBlock(new BlockDefinition(BlockType.GOLD_ORE, "gold_ore", true, false).setTextures(new BlockTextures(base + "gold_ore.png")));
        registerBlock(new BlockDefinition(BlockType.IRON_ORE, "iron_ore", true, false).setTextures(new BlockTextures(base + "iron_ore.png")));
        registerBlock(new BlockDefinition(BlockType.COAL_ORE, "coal_ore", true, false).setTextures(new BlockTextures(base + "coal_ore.png")));
        registerBlock(new BlockDefinition(BlockType.BOOKSHELF, "bookshelf", true, false).setTextures(new BlockTextures(base + "oak_planks.png", base + "oak_planks.png", base + "bookshelf.png")));
        registerBlock(new BlockDefinition(BlockType.MOSSY_COBBLESTONE, "mossy_cobblestone", true, false).setTextures(new BlockTextures(base + "mossy_cobblestone.png")));
        registerBlock(new BlockDefinition(BlockType.OBSIDIAN, "obsidian", true, false).setTextures(new BlockTextures(base + "obsidian.png")));
        registerBlock(new BlockDefinition(BlockType.ASPHALT, "asphalt", true, false).setTextures(new BlockTextures(base + "gray_concrete.png")));
        registerBlock(new BlockDefinition(BlockType.RUSTY_METAL, "rusty_metal", true, false).setTextures(new BlockTextures(base + "oxidized_copper.png")));
        registerBlock(new BlockDefinition(BlockType.GLASS, "glass", true, true).setTextures(new BlockTextures(base + "glass.png")));
        registerBlock(new BlockDefinition(BlockType.BRICKS, "bricks", true, false).setTextures(new BlockTextures(base + "bricks.png")));
        registerBlock(new BlockDefinition(BlockType.STONE_BRICKS, "stone_bricks", true, false).setTextures(new BlockTextures(base + "stone_bricks.png")));
        registerBlock(new BlockDefinition(BlockType.CYAN_CONCRETE, "cyan_concrete", true, false).setTextures(new BlockTextures(base + "cyan_concrete.png")));
        registerBlock(new BlockDefinition(BlockType.GRAY_CONCRETE, "gray_concrete", true, false).setTextures(new BlockTextures(base + "gray_concrete.png")));
        registerBlock(new BlockDefinition(BlockType.WHITE_CONCRETE, "white_concrete", true, false).setTextures(new BlockTextures(base + "white_concrete.png")));
        
        registerBlock(new SlabBlockDefinition(BlockType.STONE_SLAB, "stone_slab", true, true).setTextures(new BlockTextures(base + "stone.png")));
        registerBlock(new StairsBlockDefinition(BlockType.STONE_STAIRS, "stone_stairs", true, true).setTextures(new BlockTextures(base + "stone.png")));
        registerBlock(new SlabBlockDefinition(BlockType.BRICK_SLAB, "brick_slab", true, true).setTextures(new BlockTextures(base + "bricks.png")));
        registerBlock(new StairsBlockDefinition(BlockType.BRICK_STAIRS, "brick_stairs", true, true).setTextures(new BlockTextures(base + "bricks.png")));
    }

    public static void registerBlock(BlockDefinition def) {
        BLOCKS.put(def.getId(), def);
    }

    public static BlockDefinition getBlock(byte id) {
        return BLOCKS.getOrDefault(id, BLOCKS.get(BlockType.AIR));
    }

    public static BlockTextures getTextures(byte id) {
        BlockDefinition def = BLOCKS.get(id);
        return def != null ? def.getTextures() : null;
    }

    public static Set<String> allTextureKeys() {
        Set<String> keys = new HashSet<>();
        for (BlockDefinition def : BLOCKS.values()) {
            BlockTextures t = def.getTextures();
            if (t != null) {
                keys.add(t.getTop());
                keys.add(t.getBottom());
                keys.add(t.getNorth());
                keys.add(t.getSouth());
                keys.add(t.getEast());
                keys.add(t.getWest());
            }
        }
        return keys;
    }

    public static Map<Byte, BlockDefinition> getRegisteredBlocks() {
        return BLOCKS;
    }
}
