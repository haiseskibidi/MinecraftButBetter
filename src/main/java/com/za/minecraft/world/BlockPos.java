package com.za.minecraft.world;

public record BlockPos(int x, int y, int z) {
    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }
    
    public BlockPos north() {
        return new BlockPos(x, y, z - 1);
    }
    
    public BlockPos south() {
        return new BlockPos(x, y, z + 1);
    }
    
    public BlockPos east() {
        return new BlockPos(x + 1, y, z);
    }
    
    public BlockPos west() {
        return new BlockPos(x - 1, y, z);
    }
    
    public BlockPos up() {
        return new BlockPos(x, y + 1, z);
    }
    
    public BlockPos down() {
        return new BlockPos(x, y - 1, z);
    }
    
    public double distanceTo(BlockPos other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
