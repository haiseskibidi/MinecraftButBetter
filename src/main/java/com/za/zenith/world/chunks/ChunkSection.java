package com.za.zenith.world.chunks;

import com.za.zenith.world.blocks.Block;
import java.util.Arrays;

public class ChunkSection {
    public static final int SIZE = 16;
    private final int[] blockData; // (type << 8) | metadata
    private final byte[] lightData; // (sunlight << 4) | blocklight
    private long dirtyCounter = 0;
    private long lastMeshCounter = -1;
    private final int yOffset;

    public ChunkSection(int yOffset) {
        this.yOffset = yOffset;
        this.blockData = new int[SIZE * SIZE * SIZE];
        this.lightData = new byte[SIZE * SIZE * SIZE];
    }

    private int getIndex(int x, int y, int z) {
        return (y & 15) * (SIZE * SIZE) + z * SIZE + x;
    }

    public int getBlockData(int x, int y, int z) {
        return blockData[getIndex(x, y, z)];
    }

    public void setBlockData(int x, int y, int z, int data) {
        blockData[getIndex(x, y, z)] = data;
        dirtyCounter++;
    }

    public int getSunlight(int x, int y, int z) {
        return (lightData[getIndex(x, y, z)] >> 4) & 0xF;
    }

    public void setSunlight(int x, int y, int z, int level) {
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0x0F) | ((level & 0xF) << 4));
        dirtyCounter++;
    }

    public int getBlockLight(int x, int y, int z) {
        return lightData[getIndex(x, y, z)] & 0xF;
    }

    public void setBlockLight(int x, int y, int z, int level) {
        int idx = getIndex(x, y, z);
        lightData[idx] = (byte) ((lightData[idx] & 0xF0) | (level & 0xF));
        dirtyCounter++;
    }

    public long getDirtyCounter() { return dirtyCounter; }
    public long getLastMeshCounter() { return lastMeshCounter; }
    public void setMeshUpdated(long version) { this.lastMeshCounter = version; }
    public boolean needsMeshUpdate() { return dirtyCounter != lastMeshCounter; }
    
    public int getYOffset() { return yOffset; }

    public int[] getRawBlockData() { return blockData; }
    public byte[] getRawLightData() { return lightData; }

    public void markDirty() { dirtyCounter++; }

    // Optimization: check if section is completely air to skip meshing
    public boolean isEmpty() {
        for (int b : blockData) if (b != 0) return false;
        return true;
    }
}
