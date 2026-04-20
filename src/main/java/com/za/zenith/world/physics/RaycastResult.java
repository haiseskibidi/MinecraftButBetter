package com.za.zenith.world.physics;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.Block;
import org.joml.Vector3f;

public class RaycastResult {
    private final boolean hit;
    private final BlockPos blockPos;
    private final Block block;
    private final Vector3f hitPoint;
    private final Vector3f normal;
    private final float distance;
    
    public RaycastResult() {
        this.hit = false;
        this.blockPos = null;
        this.block = null;
        this.hitPoint = null;
        this.normal = null;
        this.distance = 0;
    }
    
    public RaycastResult(BlockPos blockPos, Block block, Vector3f hitPoint, Vector3f normal, float distance) {
        this.hit = true;
        this.blockPos = blockPos;
        this.block = block;
        this.hitPoint = new Vector3f(hitPoint);
        this.normal = new Vector3f(normal);
        this.distance = distance;
    }
    
    public boolean isHit() {
        return hit;
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    public Block getBlock() {
        return block;
    }
    
    public Vector3f getHitPoint() {
        return hitPoint;
    }
    
    public Vector3f getNormal() {
        return normal;
    }
    
    public com.za.zenith.utils.Direction getSide() {
        if (normal == null) return null;
        if (normal.y > 0.5f) return com.za.zenith.utils.Direction.UP;
        if (normal.y < -0.5f) return com.za.zenith.utils.Direction.DOWN;
        if (normal.x > 0.5f) return com.za.zenith.utils.Direction.EAST;
        if (normal.x < -0.5f) return com.za.zenith.utils.Direction.WEST;
        if (normal.z > 0.5f) return com.za.zenith.utils.Direction.NORTH;
        if (normal.z < -0.5f) return com.za.zenith.utils.Direction.SOUTH;
        return null;
    }
    
    public float getDistance() {
        return distance;
    }
}


