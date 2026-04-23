package com.za.zenith.world.chunks;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 384;
    
    private final ChunkPos position;
    private final int[] blockData; // Packed data: (type << 8) | metadata
    private final byte[] lightData; // Packed light: (sunlight << 4) | blocklight
    private final short[] heightMap;
    private long dirtyCounter = 0;
    private long lastMeshCounter = -1;
    private volatile boolean isReady = false;
    
    public Chunk(ChunkPos position) {
        this.position = position;
        this.blockData = new int[CHUNK_SIZE * CHUNK_HEIGHT * CHUNK_SIZE];
        this.lightData = new byte[CHUNK_SIZE * CHUNK_HEIGHT * CHUNK_SIZE];
        this.heightMap = new short[CHUNK_SIZE * CHUNK_SIZE];
        java.util.Arrays.fill(this.heightMap, (short)-1);
    }

    public Chunk(ChunkPos position, int[] blockData, byte[] lightData) {
        this.position = position;
        this.blockData = blockData;
        this.lightData = lightData;
        this.heightMap = new short[CHUNK_SIZE * CHUNK_SIZE];
        java.util.Arrays.fill(this.heightMap, (short)-1);
    }

    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { this.isReady = ready; }

    public synchronized int getHighestBlock(int x, int z) {
        if (x < 0 || x >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) return 0;
        int idx = z * CHUNK_SIZE + x;
        int y = heightMap[idx];
        if (y == -1) {
            for (y = CHUNK_HEIGHT - 1; y > 0; y--) {
                int data = blockData[getIndex(x, y, z)];
                int type = data >> 8;
                if (type != 0 && !com.za.zenith.world.blocks.BlockRegistry.getBlock(type).isTransparent()) {
                    heightMap[idx] = (short)y;
                    return y;
                }
            }
            heightMap[idx] = 0;
            return 0;
        }
        return y;
    }
    
    private int getIndex(int x, int y, int z) {
        return y * (CHUNK_SIZE * CHUNK_SIZE) + z * CHUNK_SIZE + x;
    }

    public synchronized int getSunlight(int x, int y, int z) {
        if (y >= CHUNK_HEIGHT) return 15;
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || z < 0 || z >= CHUNK_SIZE) return 0;
        return (lightData[getIndex(x, y, z)] >> 4) & 0xF;
    }

    public synchronized void setSunlight(int x, int y, int z, int level) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return;
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0x0F) | ((level & 0xF) << 4));
        dirtyCounter++;
    }

    public synchronized int getBlockLight(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
        return lightData[getIndex(x, y, z)] & 0xF;
    }

    public synchronized void setBlockLight(int x, int y, int z, int level) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return;
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0xF0) | (level & 0xF));
        dirtyCounter++;
    }
    
    public synchronized Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return new Block(com.za.zenith.world.blocks.Blocks.AIR.getId());
        }
        int data = blockData[getIndex(x, y, z)];
        int type = data >> 8;
        byte metadata = (byte) (data & 0xFF);
        return new Block(type, metadata);
    }
    
    public synchronized void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        int packed = (block.getType() << 8) | (block.getMetadata() & 0xFF);
        blockData[getIndex(x, y, z)] = packed;
        heightMap[z * CHUNK_SIZE + x] = -1;
        dirtyCounter++;
    }
    
    public synchronized int getRawBlockData(int x, int y, int z) {
        return blockData[getIndex(x, y, z)];
    }
    
    public ChunkPos getPosition() {
        return position;
    }
    
    public boolean needsMeshUpdate() {
        return dirtyCounter != lastMeshCounter;
    }
    
    public void setNeedsMeshUpdate(boolean needsUpdate) {
        if (needsUpdate) dirtyCounter++;
    }
    
    public void setMeshUpdated(long version) {
        this.lastMeshCounter = version;
    }

    public long getDirtyCounter() {
        return dirtyCounter;
    }

    public void setDirtyCounter(long dirtyCounter) {
        this.dirtyCounter = dirtyCounter;
    }

    public BlockPos toWorldPos(int x, int y, int z) {        return new BlockPos(
            position.x() * CHUNK_SIZE + x,
            y,
            position.z() * CHUNK_SIZE + z
        );
    }

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

    public synchronized DataSnapshot getSnapshot() {
        return new DataSnapshot(position, blockData.clone(), lightData.clone());
    }
}
