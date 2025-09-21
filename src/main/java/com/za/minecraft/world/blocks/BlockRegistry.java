package com.za.minecraft.world.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class BlockRegistry {
    private static final Map<BlockType, BlockTextures> BLOCK_TEXTURES = new HashMap<>();
    
    static {
        // Base path for minecraft textures
        String base = "minecraft/textures/block/";
        
        // Simple blocks (all faces same) - using existing textures
        BLOCK_TEXTURES.put(BlockType.DIRT, new BlockTextures(base + "dirt.png"));
        BLOCK_TEXTURES.put(BlockType.STONE, new BlockTextures(base + "stone.png"));
        BLOCK_TEXTURES.put(BlockType.LEAVES, new BlockTextures(base + "oak_leaves.png"));
        BLOCK_TEXTURES.put(BlockType.OAK_PLANKS, new BlockTextures(base + "oak_planks.png"));
        BLOCK_TEXTURES.put(BlockType.COBBLESTONE, new BlockTextures(base + "cobblestone.png"));
        BLOCK_TEXTURES.put(BlockType.BEDROCK, new BlockTextures(base + "bedrock.png"));
        BLOCK_TEXTURES.put(BlockType.SAND, new BlockTextures(base + "sand.png"));
        BLOCK_TEXTURES.put(BlockType.GRAVEL, new BlockTextures(base + "gravel.png"));
        BLOCK_TEXTURES.put(BlockType.GOLD_ORE, new BlockTextures(base + "gold_ore.png"));
        BLOCK_TEXTURES.put(BlockType.IRON_ORE, new BlockTextures(base + "iron_ore.png"));
        BLOCK_TEXTURES.put(BlockType.COAL_ORE, new BlockTextures(base + "coal_ore.png"));
        BLOCK_TEXTURES.put(BlockType.MOSSY_COBBLESTONE, new BlockTextures(base + "mossy_cobblestone.png"));
        BLOCK_TEXTURES.put(BlockType.OBSIDIAN, new BlockTextures(base + "obsidian.png"));
        
        // Complex blocks with different faces
        BLOCK_TEXTURES.put(BlockType.GRASS, new BlockTextures(
            base + "grass_block_top.png",    // top
            base + "dirt.png",               // bottom
            base + "grass_block_side.png"    // sides
        ));
        
        BLOCK_TEXTURES.put(BlockType.WOOD, new BlockTextures(
            base + "oak_log_top.png",        // top
            base + "oak_log_top.png",        // bottom  
            base + "oak_log.png"             // sides
        ));
        
        // Bookshelf - special case with different top/bottom/sides
        BLOCK_TEXTURES.put(BlockType.BOOKSHELF, new BlockTextures(
            base + "oak_planks.png",         // top
            base + "oak_planks.png",         // bottom
            base + "bookshelf.png"           // sides
        ));
    }
    
    public static BlockTextures getTextures(BlockType type) {
        return BLOCK_TEXTURES.get(type);
    }
    
    public static void registerBlock(BlockType type, BlockTextures textures) {
        BLOCK_TEXTURES.put(type, textures);
    }
    
    // Helper methods for easy registration
    public static void registerSimpleBlock(BlockType type, String texture) {
        registerBlock(type, new BlockTextures(texture));
    }
    
    public static void registerTopBottomSidesBlock(BlockType type, String top, String bottom, String sides) {
        registerBlock(type, new BlockTextures(top, bottom, sides));
    }

    public static Set<String> allTextureKeys() {
        Set<String> keys = new HashSet<>();
        for (BlockTextures t : BLOCK_TEXTURES.values()) {
            keys.add(t.getTop());
            keys.add(t.getBottom());
            keys.add(t.getNorth());
            keys.add(t.getSouth());
            keys.add(t.getEast());
            keys.add(t.getWest());
        }
        return keys;
    }
}
