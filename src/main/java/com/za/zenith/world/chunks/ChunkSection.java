package com.za.zenith.world.chunks;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkSection {
    public static final int SECTION_SIZE = 16;
    public static final int SECTION_VOLUME = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE;
    
    private final short[] blockIndices;
    private final byte[] lightData;
    private int nonEmptyBlockCount;
    private final AtomicLong dirtyCounter = new AtomicLong(0);
    private long lastMeshCounter = -1;
    
    public ChunkSection() {
        this.blockIndices = new short[SECTION_VOLUME];
        this.lightData = new byte[SECTION_VOLUME];
        this.nonEmptyBlockCount = 0;
    }
    
    private int getIndex(int x, int y, int z) {
        return y * (SECTION_SIZE * SECTION_SIZE) + z * SECTION_SIZE + x;
    }
    
    public short getBlockIndex(int x, int y, int z) {
        return blockIndices[getIndex(x, y, z)];
    }
    
    public void setBlockIndex(int x, int y, int z, short index, boolean wasAir, boolean isAir) {
        blockIndices[getIndex(x, y, z)] = index;
        if (wasAir && !isAir) {
            nonEmptyBlockCount++;
        } else if (!wasAir && isAir) {
            nonEmptyBlockCount--;
        }
        markDirty();
    }
    
    public int getSunlight(int x, int y, int z) {
        return (lightData[getIndex(x, y, z)] >> 4) & 0xF;
    }

    public void setSunlight(int x, int y, int z, int level) {
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0x0F) | ((level & 0xF) << 4));
        markDirty();
    }

    public int getBlockLight(int x, int y, int z) {
        return lightData[getIndex(x, y, z)] & 0xF;
    }

    public void setBlockLight(int x, int y, int z, int level) {
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0xF0) | (level & 0xF));
        markDirty();
    }
    
    public boolean isEmpty() {
        return nonEmptyBlockCount == 0;
    }
    
    public long getDirtyCounter() {
        return dirtyCounter.get();
    }
    
    public void markDirty() {
        dirtyCounter.incrementAndGet();
    }
    
    public boolean needsMeshUpdate() {
        return dirtyCounter.get() != lastMeshCounter;
    }
    
    public void setMeshUpdated(long version) {
        this.lastMeshCounter = version;
    }
    
    public void setNeedsMeshUpdate(boolean needsUpdate) {
        if (needsUpdate) markDirty();
    }

    public short[] getBlockIndices() {
        return blockIndices;
    }

    public byte[] getLightData() {
        return lightData;
    }
    
    public void fillLightData(byte[] data, int sourceOffset) {
        System.arraycopy(data, sourceOffset, this.lightData, 0, SECTION_VOLUME);
    }
}
