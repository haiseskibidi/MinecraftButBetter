package com.za.minecraft.world.physics;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
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
    
    public float getDistance() {
        return distance;
    }
}
