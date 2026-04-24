package com.za.zenith.world.chunks;

public record ChunkPos(int x, int z) {
    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
    
    public static int unpackX(long packed) {
        return (int) (packed >> 32);
    }
    
    public static int unpackZ(long packed) {
        return (int) packed;
    }
    
    public long pack() {
        return pack(x, z);
    }
    
    public static ChunkPos fromBlockPos(int blockX, int blockZ) {
        return new ChunkPos(
            Math.floorDiv(blockX, Chunk.CHUNK_SIZE),
            Math.floorDiv(blockZ, Chunk.CHUNK_SIZE)
        );
    }
    
    public ChunkPos offset(int dx, int dz) {
        return new ChunkPos(x + dx, z + dz);
    }
    
    public double distanceTo(ChunkPos other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}


