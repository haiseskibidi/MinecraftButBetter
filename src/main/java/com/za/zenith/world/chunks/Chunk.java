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
    private final short[] blockIndices; 
    private final java.util.List<Integer> palette = new java.util.ArrayList<>();
    private final byte[] lightData; // Packed light: (sunlight << 4) | blocklight
    private final short[] heightMap;
    private final AtomicLong dirtyCounter = new AtomicLong(0);
    private long lastMeshCounter = -1;
    private volatile boolean isReady = false;
    
    private final float firstSpawnTime;
    private long lastSeenFrame = 0;

    public long getLastSeenFrame() { return lastSeenFrame; }
    public void setLastSeenFrame(long frame) { this.lastSeenFrame = frame; }
    public float getFirstSpawnTime() { return firstSpawnTime; }

    public Chunk(ChunkPos position) {
        this.position = position;
        this.blockIndices = new short[CHUNK_SIZE * CHUNK_HEIGHT * CHUNK_SIZE];
        this.lightData = new byte[CHUNK_SIZE * CHUNK_HEIGHT * CHUNK_SIZE];
        this.heightMap = new short[CHUNK_SIZE * CHUNK_SIZE];
        java.util.Arrays.fill(this.heightMap, (short)-1);
        
        // Initial palette: Air at index 0
        palette.add(0);
        this.firstSpawnTime = (float)(org.lwjgl.glfw.GLFW.glfwGetTime() % 3600.0);
    }

    public Chunk(ChunkPos position, int[] rawBlockData, byte[] lightData) {
        this(position);
        System.arraycopy(lightData, 0, this.lightData, 0, lightData.length);
        for (int i = 0; i < rawBlockData.length; i++) {
            setBlockByIndex(i, rawBlockData[i]);
        }
    }

    private void setBlockByIndex(int index, int packedData) {
        short paletteIndex = -1;
        for (int i = 0; i < palette.size(); i++) {
            if (palette.get(i) == packedData) {
                paletteIndex = (short) i;
                break;
            }
        }
        if (paletteIndex == -1) {
            paletteIndex = (short) palette.size();
            palette.add(packedData);
        }
        blockIndices[index] = paletteIndex;
    }

    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { this.isReady = ready; }

    public int getHighestBlock(int x, int z) {
        if (x < 0 || x >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) return 0;
        int idx = z * CHUNK_SIZE + x;
        int y = heightMap[idx];
        if (y == -1) {
            for (y = CHUNK_HEIGHT - 1; y > 0; y--) {
                int type = getBlockType(x, y, z);
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
        Block b = FLYWEIGHT_BLOCK.get();
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            b.setType(com.za.zenith.world.blocks.Blocks.AIR.getId());
            b.setMetadata((byte)0);
            return b;
        }
        int data = getRawBlockData(x, y, z);
        b.setType(data >> 8);
        b.setMetadata((byte)(data & 0xFF));
        return b;
    }

    public int getBlockType(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
        return getRawBlockData(x, y, z) >> 8;
    }

    public com.za.zenith.world.BlockPos toWorldPos(int localX, int localY, int localZ) {
        return new com.za.zenith.world.BlockPos(
            position.x() * CHUNK_SIZE + localX,
            localY,
            position.z() * CHUNK_SIZE + localZ
        );
    }

    public byte getBlockMetadata(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
        return (byte)(getRawBlockData(x, y, z) & 0xFF);
    }
    
    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        int packed = (block.getType() << 8) | (block.getMetadata() & 0xFF);
        setBlockByIndex(getIndex(x, y, z), packed);
        heightMap[z * CHUNK_SIZE + x] = -1;
        dirtyCounter.incrementAndGet();
    }
    
    public int getRawBlockData(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) return 0;
        return palette.get(blockIndices[getIndex(x, y, z)]);
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

    public int[] getBlockData() { 
        int[] data = new int[blockIndices.length];
        for (int i = 0; i < blockIndices.length; i++) {
            data[i] = palette.get(blockIndices[i]);
        }
        return data; 
    }
    
    public byte[] getLightData() { return lightData; }

    public synchronized DataSnapshot getSnapshot(int[] outBlockData, byte[] outLightData) {
        for (int i = 0; i < blockIndices.length; i++) {
            outBlockData[i] = palette.get(blockIndices[i]);
        }
        System.arraycopy(lightData, 0, outLightData, 0, lightData.length);
        return new DataSnapshot(position, outBlockData, outLightData);
    }
}
