package com.za.minecraft.world.chunks;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 384;
    
    private static final Block AIR_BLOCK = new Block(BlockType.AIR); // Cached air block
    
    private final ChunkPos position;
    private final Block[][][] blocks;
    private boolean needsMeshUpdate = true;
    
    public Chunk(ChunkPos position) {
        this.position = position;
        this.blocks = new Block[CHUNK_SIZE][CHUNK_HEIGHT][CHUNK_SIZE];
        
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    blocks[x][y][z] = AIR_BLOCK;
                }
            }
        }
    }
    
    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return AIR_BLOCK; // Use cached air block instead of creating new ones
        }
        return blocks[x][y][z];
    }
    
    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        blocks[x][y][z] = block;
        needsMeshUpdate = true;
    }
    
    public ChunkPos getPosition() {
        return position;
    }
    
    public boolean needsMeshUpdate() {
        return needsMeshUpdate;
    }
    
    public void setMeshUpdated() {
        needsMeshUpdate = false;
    }
    
    public BlockPos toWorldPos(int x, int y, int z) {
        return new BlockPos(
            position.x() * CHUNK_SIZE + x,
            y,
            position.z() * CHUNK_SIZE + z
        );
    }
}
