package com.za.minecraft.entities;

import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    
    private Block[] hotbarSlots;
    private int selectedSlot;
    
    public Inventory() {
        this.hotbarSlots = new Block[HOTBAR_SIZE];
        this.selectedSlot = 0;
        
        // Заполняем хотбар доступными блоками
        for (int i = 0; i < Math.min(HOTBAR_SIZE, AVAILABLE_BLOCKS.length); i++) {
            hotbarSlots[i] = new Block(AVAILABLE_BLOCKS[i]);
        }
        // Остальные слоты оставляем null (пустые)
    }
    
    public Block getSelectedBlock() {
        return hotbarSlots[selectedSlot];
    }
    
    public void setSelectedBlock(Block block) {
        hotbarSlots[selectedSlot] = block;
    }
    
    public void selectBlockType(BlockType type) {
        hotbarSlots[selectedSlot] = new Block(type);
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            this.selectedSlot = slot;
        }
    }
    
    public Block getBlockInSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            return hotbarSlots[slot];
        }
        return null;
    }
    
    public void setBlockInSlot(int slot, Block block) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            hotbarSlots[slot] = block;
        }
    }
    
    // Available block types for building
    private static final BlockType[] AVAILABLE_BLOCKS = {
        BlockType.STONE, BlockType.DIRT, BlockType.GRASS, BlockType.WOOD, BlockType.LEAVES,
        BlockType.OAK_PLANKS, BlockType.COBBLESTONE, BlockType.SAND, BlockType.GRAVEL,
        BlockType.GOLD_ORE, BlockType.IRON_ORE, BlockType.COAL_ORE, BlockType.BOOKSHELF,
        BlockType.MOSSY_COBBLESTONE, BlockType.OBSIDIAN
    };
    
    // Циклическое переключение между слотами хотбара  
    public void nextBlock() {
        selectedSlot = (selectedSlot + 1) % HOTBAR_SIZE;
        Block currentBlock = getSelectedBlock();
        String blockName = (currentBlock != null) ? currentBlock.getType().getName() : "EMPTY";
        com.za.minecraft.utils.Logger.info("Selected slot %d: %s", selectedSlot, blockName);
    }
    
    public void previousBlock() {
        selectedSlot = (selectedSlot - 1 + HOTBAR_SIZE) % HOTBAR_SIZE;
        Block currentBlock = getSelectedBlock();
        String blockName = (currentBlock != null) ? currentBlock.getType().getName() : "EMPTY";
        com.za.minecraft.utils.Logger.info("Selected slot %d: %s", selectedSlot, blockName);
    }
}
