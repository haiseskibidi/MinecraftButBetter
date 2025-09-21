package com.za.minecraft.world.chunks;

public record ChunkPos(int x, int z) {
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
