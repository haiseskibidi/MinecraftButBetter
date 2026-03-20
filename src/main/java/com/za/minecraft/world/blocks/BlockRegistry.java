package com.za.minecraft.world.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class BlockRegistry {
    private static final Map<Byte, BlockDefinition> BLOCKS = new HashMap<>();

    static {
        String base = "minecraft/textures/block/";
        
        registerBlock(new BlockDefinition(BlockType.AIR, "block.minecraft.air", false, false));
        registerBlock(new BlockDefinition(BlockType.GRASS, "block.minecraft.grass_block", true, false).setHardness(0.6f).setTextures(new BlockTextures(base + "grass_block_top.png", base + "dirt.png", base + "grass_block_side.png")));
        registerBlock(new BlockDefinition(BlockType.DIRT, "block.minecraft.dirt", true, false).setHardness(0.5f).setTextures(new BlockTextures(base + "dirt.png")));
        registerBlock(new BlockDefinition(BlockType.STONE, "block.minecraft.stone", true, false).setHardness(1.5f).setTextures(new BlockTextures(base + "stone.png")));
        registerBlock(new BlockDefinition(BlockType.WOOD, "block.minecraft.oak_log", true, false).setHardness(2.0f).setTextures(new BlockTextures(base + "oak_log_top.png", base + "oak_log_top.png", base + "oak_log.png")));
        registerBlock(new BlockDefinition(BlockType.LEAVES, "block.minecraft.oak_leaves", true, true).setHardness(0.2f).setTextures(new BlockTextures(base + "oak_leaves.png")));
        registerBlock(new BlockDefinition(BlockType.OAK_PLANKS, "block.minecraft.oak_planks", true, false).setHardness(2.0f).setTextures(new BlockTextures(base + "oak_planks.png")));
        registerBlock(new BlockDefinition(BlockType.COBBLESTONE, "block.minecraft.cobblestone", true, false).setHardness(2.0f).setTextures(new BlockTextures(base + "cobblestone.png")));
        registerBlock(new BlockDefinition(BlockType.BEDROCK, "block.minecraft.bedrock", true, false).setHardness(-1.0f).setTextures(new BlockTextures(base + "bedrock.png")));
        registerBlock(new BlockDefinition(BlockType.SAND, "block.minecraft.sand", true, false).setHardness(0.5f).setTextures(new BlockTextures(base + "sand.png")));
        registerBlock(new BlockDefinition(BlockType.GRAVEL, "block.minecraft.gravel", true, false).setHardness(0.6f).setTextures(new BlockTextures(base + "gravel.png")));
        registerBlock(new BlockDefinition(BlockType.GOLD_ORE, "block.minecraft.gold_ore", true, false).setHardness(3.0f).setTextures(new BlockTextures(base + "gold_ore.png")));
        registerBlock(new BlockDefinition(BlockType.IRON_ORE, "block.minecraft.iron_ore", true, false).setHardness(3.0f).setTextures(new BlockTextures(base + "iron_ore.png")));
        registerBlock(new BlockDefinition(BlockType.COAL_ORE, "block.minecraft.coal_ore", true, false).setHardness(3.0f).setTextures(new BlockTextures(base + "coal_ore.png")));
        registerBlock(new BlockDefinition(BlockType.BOOKSHELF, "block.minecraft.bookshelf", true, false).setHardness(1.5f).setTextures(new BlockTextures(base + "oak_planks.png", base + "oak_planks.png", base + "bookshelf.png")));
        registerBlock(new BlockDefinition(BlockType.MOSSY_COBBLESTONE, "block.minecraft.mossy_cobblestone", true, false).setHardness(2.0f).setTextures(new BlockTextures(base + "mossy_cobblestone.png")));
        registerBlock(new BlockDefinition(BlockType.OBSIDIAN, "block.minecraft.obsidian", true, false).setHardness(50.0f).setTextures(new BlockTextures(base + "obsidian.png")));
        registerBlock(new BlockDefinition(BlockType.ASPHALT, "block.minecraft.asphalt", true, false).setHardness(2.0f).setTextures(new BlockTextures(base + "gray_concrete.png")));
        registerBlock(new BlockDefinition(BlockType.RUSTY_METAL, "block.minecraft.rusty_metal", true, false).setHardness(5.0f).setTextures(new BlockTextures(base + "oxidized_copper.png")));
        registerBlock(new BlockDefinition(BlockType.GLASS, "block.minecraft.glass", true, true).setHardness(0.3f).setTextures(new BlockTextures(base + "glass.png")));
        registerBlock(new BlockDefinition(BlockType.BRICKS, "block.minecraft.bricks", true, false).setHardness(2.0f).setTextures(new BlockTextures(base + "bricks.png")));
        registerBlock(new BlockDefinition(BlockType.STONE_BRICKS, "block.minecraft.stone_bricks", true, false).setHardness(2.0f).setTextures(new BlockTextures(base + "stone_bricks.png")));
        registerBlock(new BlockDefinition(BlockType.CYAN_CONCRETE, "block.minecraft.cyan_concrete", true, false).setHardness(1.8f).setTextures(new BlockTextures(base + "cyan_concrete.png")));
        registerBlock(new BlockDefinition(BlockType.GRAY_CONCRETE, "block.minecraft.gray_concrete", true, false).setHardness(1.8f).setTextures(new BlockTextures(base + "gray_concrete.png")));
        registerBlock(new BlockDefinition(BlockType.WHITE_CONCRETE, "block.minecraft.white_concrete", true, false).setHardness(1.8f).setTextures(new BlockTextures(base + "white_concrete.png")));
        
        registerBlock(new SlabBlockDefinition(BlockType.STONE_SLAB, "block.minecraft.stone_slab", true, true).setHardness(1.5f).setTextures(new BlockTextures(base + "stone.png")));
        registerBlock(new StairsBlockDefinition(BlockType.STONE_STAIRS, "block.minecraft.stone_stairs", true, true).setHardness(1.5f).setTextures(new BlockTextures(base + "stone.png")));
        registerBlock(new SlabBlockDefinition(BlockType.BRICK_SLAB, "block.minecraft.brick_slab", true, true).setHardness(2.0f).setTextures(new BlockTextures(base + "bricks.png")));
        registerBlock(new StairsBlockDefinition(BlockType.BRICK_STAIRS, "block.minecraft.brick_stairs", true, true).setHardness(2.0f).setTextures(new BlockTextures(base + "bricks.png")));
        registerBlock(new BlockDefinition(BlockType.CAMPFIRE, "block.minecraft.campfire", true, true).setHardness(1.0f).setTextures(new BlockTextures(base + "cobblestone.png", base + "cobblestone.png", base + "oak_log.png")));
        registerBlock(new GeneratorBlockDefinition(BlockType.GENERATOR, "block.minecraft.generator", true, false).setHardness(3.5f).setTextures(new BlockTextures(base + "blast_furnace_top.png", base + "blast_furnace_top.png", base + "blast_furnace_front.png", base + "blast_furnace_front.png", base + "blast_furnace_side.png", base + "blast_furnace_side.png")));
        registerBlock(new CableBlockDefinition(BlockType.CABLE, "block.minecraft.cable", true, false).setHardness(1.0f).setTextures(new BlockTextures(base + "black_concrete.png")));
        registerBlock(new LampBlockDefinition(BlockType.ELECTRIC_LAMP, "block.minecraft.electric_lamp", true, false).setHardness(0.5f).setTextures(new BlockTextures(base + "sea_lantern.png")));
        registerBlock(new BatteryBlockDefinition(BlockType.BATTERY, "block.minecraft.battery", true, false).setHardness(3.0f).setTextures(new BlockTextures(base + "blast_furnace_top.png", base + "blast_furnace_top.png", base + "iron_block.png", base + "iron_block.png", base + "iron_block.png", base + "iron_block.png")));
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
