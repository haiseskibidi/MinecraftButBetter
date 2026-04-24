package com.za.zenith.world.chunks;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 384;
    
    private final ChunkPos position;
    private final int[] blockData; // Packed data: (type << 8) | metadata
    private final byte[] lightData; // Packed light: (sunlight << 4) | blocklight
    private final short[] heightMap;
    private final AtomicLong dirtyCounter = new AtomicLong(0);
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

    public int getHighestBlock(int x, int z) {
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

    public int getSunlight(int x, int y, int z) {
        if (y >= CHUNK_HEIGHT) return 15;
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || z < 0 || z >= CHUNK_SIZE) return 0;
        return (lightData[getIndex(x, y, z)] >> 4) & 0xF;
    }

    public void setSunlight(int x, int y, int z, int level) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return;
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0x0F) | ((level & 0xF) << 4));
        dirtyCounter.incrementAndGet();
    }

    public int getBlockLight(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
        return lightData[getIndex(x, y, z)] & 0xF;
    }

    public void setBlockLight(int x, int y, int z, int level) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return;
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0xF0) | (level & 0xF));
        dirtyCounter.incrementAndGet();
    }
    
    private static final ThreadLocal<Block> FLYWEIGHT_BLOCK = ThreadLocal.withInitial(() -> new Block(0));

    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            Block b = FLYWEIGHT_BLOCK.get();
            b.setType(com.za.zenith.world.blocks.Blocks.AIR.getId());
            b.setMetadata((byte)0);
            return b;
        }
        int data = blockData[getIndex(x, y, z)];
        Block b = FLYWEIGHT_BLOCK.get();
        b.setType(data >> 8);
        b.setMetadata((byte)(data & 0xFF));
        return b;
    }

    public int getBlockType(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
        return blockData[getIndex(x, y, z)] >> 8;
    }

    public byte getBlockMetadata(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
        return (byte)(blockData[getIndex(x, y, z)] & 0xFF);
    }
    
    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        int packed = (block.getType() << 8) | (block.getMetadata() & 0xFF);
        blockData[getIndex(x, y, z)] = packed;
        heightMap[z * CHUNK_SIZE + x] = -1;
        dirtyCounter.incrementAndGet();
    }
    
    public int getRawBlockData(int x, int y, int z) {
        return blockData[getIndex(x, y, z)];
    }
    
    public ChunkPos getPosition() {
        return position;
    }
    
    public boolean needsMeshUpdate() {
        return dirtyCounter.get() != lastMeshCounter;
    }
    
    public void setNeedsMeshUpdate(boolean needsUpdate) {
        if (needsUpdate) dirtyCounter.incrementAndGet();
    }
    
    public void setMeshUpdated(long version) {
        this.lastMeshCounter = version;
    }

    public long getDirtyCounter() {
        return dirtyCounter.get();
    }

    public void setDirtyCounter(long dirtyCounterVal) {
        this.dirtyCounter.set(dirtyCounterVal);
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

    public synchronized DataSnapshot getSnapshot(int[] outBlockData, byte[] outLightData) {
        System.arraycopy(blockData, 0, outBlockData, 0, blockData.length);
        System.arraycopy(lightData, 0, outLightData, 0, lightData.length);
        return new DataSnapshot(position, outBlockData, outLightData);
    }
}
