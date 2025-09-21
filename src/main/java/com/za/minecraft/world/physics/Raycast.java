package com.za.minecraft.world.physics;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import org.joml.Vector3f;

public class Raycast {
    private static final float MAX_REACH_DISTANCE = 5.0f;
    private static final float STEP_SIZE = 0.1f;
    
    public static RaycastResult raycast(World world, Vector3f origin, Vector3f direction) {
        Vector3f dir = new Vector3f(direction).normalize();
        if (dir.lengthSquared() == 0) {
            return new RaycastResult();
        }

        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        int stepX = dir.x > 0 ? 1 : (dir.x < 0 ? -1 : 0);
        int stepY = dir.y > 0 ? 1 : (dir.y < 0 ? -1 : 0);
        int stepZ = dir.z > 0 ? 1 : (dir.z < 0 ? -1 : 0);

        float nextVoxelBoundaryX = stepX > 0 ? (x + 1) : x;
        float nextVoxelBoundaryY = stepY > 0 ? (y + 1) : y;
        float nextVoxelBoundaryZ = stepZ > 0 ? (z + 1) : z;

        float tMaxX = stepX != 0 ? (nextVoxelBoundaryX - origin.x) / dir.x : Float.POSITIVE_INFINITY;
        float tMaxY = stepY != 0 ? (nextVoxelBoundaryY - origin.y) / dir.y : Float.POSITIVE_INFINITY;
        float tMaxZ = stepZ != 0 ? (nextVoxelBoundaryZ - origin.z) / dir.z : Float.POSITIVE_INFINITY;

        float tDeltaX = stepX != 0 ? Math.abs(1f / dir.x) : Float.POSITIVE_INFINITY;
        float tDeltaY = stepY != 0 ? Math.abs(1f / dir.y) : Float.POSITIVE_INFINITY;
        float tDeltaZ = stepZ != 0 ? Math.abs(1f / dir.z) : Float.POSITIVE_INFINITY;

        float maxT = MAX_REACH_DISTANCE;
        Vector3f lastNormal = new Vector3f();

        for (;;) {
            // Check current cell
            Block block = world.getBlock(x, y, z);
            if (!block.isAir()) {
                Vector3f hitPoint = new Vector3f(origin).fma(Math.min(Math.min(tMaxX, tMaxY), tMaxZ), dir);
                return new RaycastResult(new BlockPos(x, y, z), block, hitPoint, lastNormal, hitPoint.distance(origin));
            }

            // Step to next cell
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > maxT) break;
                    x += stepX;
                    tMaxX += tDeltaX;
                    lastNormal.set(-stepX, 0, 0);
                } else {
                    if (tMaxZ > maxT) break;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    lastNormal.set(0, 0, -stepZ);
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > maxT) break;
                    y += stepY;
                    tMaxY += tDeltaY;
                    lastNormal.set(0, -stepY, 0);
                } else {
                    if (tMaxZ > maxT) break;
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                    lastNormal.set(0, 0, -stepZ);
                }
            }
        }

        return new RaycastResult();
    }
    
    private static Vector3f calculateNormal(Vector3f hitPoint, BlockPos blockPos) {
        // Определяем какая грань блока была поражена
        float x = hitPoint.x - blockPos.x();
        float y = hitPoint.y - blockPos.y();
        float z = hitPoint.z - blockPos.z();
        
        float absX = Math.abs(x - 0.5f);
        float absY = Math.abs(y - 0.5f);
        float absZ = Math.abs(z - 0.5f);
        
        // Находим ось с максимальным отклонением от центра блока
        if (absX > absY && absX > absZ) {
            // Попадание по грани X
            return new Vector3f(x > 0.5f ? 1 : -1, 0, 0);
        } else if (absY > absZ) {
            // Попадание по грани Y
            return new Vector3f(0, y > 0.5f ? 1 : -1, 0);
        } else {
            // Попадание по грани Z
            return new Vector3f(0, 0, z > 0.5f ? 1 : -1);
        }
    }
}
