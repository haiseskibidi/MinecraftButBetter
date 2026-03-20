package com.za.minecraft.utils;

import com.za.minecraft.world.BlockPos;

/**
 * Перечисление 6 сторон блока.
 * ПОРЯДОК ВАЖЕН: должен соответствовать индексам граней в ChunkMeshGenerator.
 */
public enum Direction {
    NORTH(0, 0, 1),   // Face 0 (+Z)
    SOUTH(0, 0, -1),  // Face 1 (-Z)
    EAST(1, 0, 0),    // Face 2 (+X)
    WEST(-1, 0, 0),   // Face 3 (-X)
    UP(0, 1, 0),      // Face 4 (+Y)
    DOWN(0, -1, 0);   // Face 5 (-Y)

    private final int dx, dy, dz;

    Direction(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public int getDx() { return dx; }
    public int getDy() { return dy; }
    public int getDz() { return dz; }

    public BlockPos offset(BlockPos pos) {
        return new BlockPos(pos.x() + dx, pos.y() + dy, pos.z() + dz);
    }
}
