package com.za.zenith.world.chunks;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 384;
    
    private final ChunkPos position;
    private final int[] blockData; // Packed data: (type << 8) | metadata
    private final byte[] lightData; // Packed light: (sunlight << 4) | blocklight
    private boolean needsMeshUpdate = true;
    
    public Chunk(ChunkPos position) {
        this.position = position;
        this.blockData = new int[CHUNK_SIZE * CHUNK_HEIGHT * CHUNK_SIZE];
        this.lightData = new byte[CHUNK_SIZE * CHUNK_HEIGHT * CHUNK_SIZE];
    }
    
    private int getIndex(int x, int y, int z) {
        return y * (CHUNK_SIZE * CHUNK_SIZE) + z * CHUNK_SIZE + x;
    }

    public int getSunlight(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 15;
        return (lightData[getIndex(x, y, z)] >> 4) & 0xF;
    }

    public void setSunlight(int x, int y, int z, int level) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return;
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0x0F) | ((level & 0xF) << 4));
        needsMeshUpdate = true;
    }

    public int getBlockLight(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
        return lightData[getIndex(x, y, z)] & 0xF;
    }

    public void setBlockLight(int x, int y, int z, int level) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return;
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0xF0) | (level & 0xF));
        needsMeshUpdate = true;
    }
    
    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return new Block(com.za.zenith.world.blocks.Blocks.AIR.getId());
        }
        int data = blockData[getIndex(x, y, z)];
        int type = data >> 8;
        byte metadata = (byte) (data & 0xFF);
        return new Block(type, metadata);
    }
    
    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        int packed = (block.getType() << 8) | (block.getMetadata() & 0xFF);
        blockData[getIndex(x, y, z)] = packed;
        needsMeshUpdate = true;
    }
    
    public int getRawBlockData(int x, int y, int z) {
        return blockData[getIndex(x, y, z)];
    }
    
    public ChunkPos getPosition() {
        return position;
    }
    
    public boolean needsMeshUpdate() {
        return needsMeshUpdate;
    }
    
    public void setNeedsMeshUpdate(boolean needsUpdate) {
        this.needsMeshUpdate = needsUpdate;
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

    /**
     * Снапшот данных чанка для асинхронной генерации меша.
     * Содержит глубокую копию blockData и lightData.
     */
    public record DataSnapshot(ChunkPos position, int[] blockData, byte[] lightData) {
        public int getRawBlockData(int x, int y, int z) {
            if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
            return blockData[y * (CHUNK_SIZE * CHUNK_SIZE) + z * CHUNK_SIZE + x];
        }
        
        public int getSunlight(int x, int y, int z) {
            if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 15;
            return (lightData[y * (CHUNK_SIZE * CHUNK_SIZE) + z * CHUNK_SIZE + x] >> 4) & 0xF;
        }

        public int getBlockLight(int x, int y, int z) {
            if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
            return lightData[y * (CHUNK_SIZE * CHUNK_SIZE) + z * CHUNK_SIZE + x] & 0xF;
        }
    }

    public int[] getBlockData() { return blockData; }
    public byte[] getLightData() { return lightData; }

    public DataSnapshot getSnapshot() {
        return new DataSnapshot(position, blockData.clone(), lightData.clone());
    }
}


