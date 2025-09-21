package com.za.minecraft.entities;

import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;

public class Inventory {
    private Block selectedBlock;
    
    public Inventory() {
        this.selectedBlock = new Block(BlockType.STONE); // По умолчанию выбран камень
    }
    
    public Block getSelectedBlock() {
        return selectedBlock;
    }
    
    public void setSelectedBlock(Block block) {
        this.selectedBlock = block;
    }
    
    public void selectBlockType(BlockType type) {
        this.selectedBlock = new Block(type);
    }
    
    // Available block types for building
    private static final BlockType[] AVAILABLE_BLOCKS = {
        BlockType.STONE, BlockType.DIRT, BlockType.GRASS, BlockType.WOOD, BlockType.LEAVES,
        BlockType.OAK_PLANKS, BlockType.COBBLESTONE, BlockType.SAND, BlockType.GRAVEL,
        BlockType.GOLD_ORE, BlockType.IRON_ORE, BlockType.COAL_ORE, BlockType.BOOKSHELF,
        BlockType.MOSSY_COBBLESTONE, BlockType.OBSIDIAN
    };
    
    // Циклическое переключение между типами блоков
    public void nextBlock() {
        for (int i = 0; i < AVAILABLE_BLOCKS.length; i++) {
            if (AVAILABLE_BLOCKS[i] == selectedBlock.getType()) {
                selectedBlock = new Block(AVAILABLE_BLOCKS[(i + 1) % AVAILABLE_BLOCKS.length]);
                com.za.minecraft.utils.Logger.info("Selected block: %s", selectedBlock.getType().getName());
                return;
            }
        }
    }
    
    public void previousBlock() {
        for (int i = 0; i < AVAILABLE_BLOCKS.length; i++) {
            if (AVAILABLE_BLOCKS[i] == selectedBlock.getType()) {
                int prevIndex = (i - 1 + AVAILABLE_BLOCKS.length) % AVAILABLE_BLOCKS.length;
                selectedBlock = new Block(AVAILABLE_BLOCKS[prevIndex]);
                com.za.minecraft.utils.Logger.info("Selected block: %s", selectedBlock.getType().getName());
                return;
            }
        }
    }
}
