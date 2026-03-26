package com.za.minecraft.entities;

import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.physics.AABB;
import org.joml.Vector3f;

/**
 * Base class for all entities in the game (Players, Mobs, Items, etc.).
 * Handles physics, movement, and collisions.
 */
public abstract class Entity {
    protected Vector3f position;
    protected final Vector3f velocity;
    protected final Vector3f rotation;
    protected com.za.minecraft.world.physics.AABB boundingBox;
    
    protected boolean onGround = false;
    protected boolean flying = false;
    protected boolean removed = false;
    
    protected static final float GRAVITY = -28.0f;
    protected static final float TERMINAL_VELOCITY = -50.0f;

    public Entity(Vector3f position, float width, float height) {
        this.position = new Vector3f(position);
        this.velocity = new Vector3f();
        this.rotation = new Vector3f();
        setBoundingBox(width, height);
    }

    protected void setBoundingBox(float width, float height) {
        this.boundingBox = new AABB(
            -width / 2, 0, -width / 2,
            width / 2, height, width / 2
        );
    }

    public void setRemoved() {
        this.removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void update(float deltaTime, World world) {
        if (!flying) {
            velocity.y = Math.max(velocity.y + GRAVITY * deltaTime, TERMINAL_VELOCITY);
        }
        
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        // Snap very small residual velocities to zero
        if (Math.abs(velocity.x) < 0.005f) velocity.x = 0f;
        if (Math.abs(velocity.z) < 0.005f) velocity.z = 0f;
        if (onGround && Math.abs(velocity.y) < 0.005f) velocity.y = 0f;
    }

    protected void move(World world, float dx, float dy, float dz) {
        float originalDy = dy;
        AABB currentBox = boundingBox.offset(position);

        // --- Vertical movement with collision ---
        if (dy != 0) {
            // Reset onGround only if we are moving significantly upwards (jump)
            if (dy > 0.001f) onGround = false; 

            int minX = (int) Math.floor(currentBox.getMin().x);
            int maxX = (int) Math.floor(currentBox.getMax().x);
            int minZ = (int) Math.floor(currentBox.getMin().z);
            int maxZ = (int) Math.floor(currentBox.getMax().z);
            int minY = (int) Math.floor(currentBox.getMin().y + Math.min(0, dy));
            int maxY = (int) Math.floor(currentBox.getMax().y + Math.max(0, dy));

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        Block block = world.getBlock(x, y, z);
                        if (!block.isAir() && block.isSolid()) {
                            com.za.minecraft.world.physics.VoxelShape shape = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType()).getShape(block.getMetadata());
                            if (shape != null) {
                                for (AABB box : shape.getBoxes()) {
                                    AABB blockBox = box.offset(x, y, z);
                                    AABB testBox = currentBox.offset(0, dy, 0);
                                    if (testBox.intersects(blockBox)) {
                                        if (dy > 0) {
                                            dy = blockBox.getMin().y - currentBox.getMax().y - 0.001f;
                                        } else {
                                            dy = blockBox.getMax().y - currentBox.getMin().y + 0.001f;
                                            onGround = true; 
                                        }
                                        velocity.y = 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            position.y += dy;
            currentBox = boundingBox.offset(position);
        }

        // If we were falling but didn't hit ground this frame, we are no longer on ground
        if (originalDy < -0.001f && onGround && Math.abs(dy - originalDy) < 0.0001f) {
            onGround = false;
        }

        // --- Horizontal movement with collision (X) ---
        if (dx != 0) {
            int minX = (int) Math.floor(currentBox.getMin().x + Math.min(0, dx));
            int maxX = (int) Math.floor(currentBox.getMax().x + Math.max(0, dx));
            int minZ = (int) Math.floor(currentBox.getMin().z);
            int maxZ = (int) Math.floor(currentBox.getMax().z);
            int minY = (int) Math.floor(currentBox.getMin().y);
            int maxY = (int) Math.floor(currentBox.getMax().y);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        Block block = world.getBlock(x, y, z);
                        if (!block.isAir() && block.isSolid()) {
                            com.za.minecraft.world.physics.VoxelShape shape = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType()).getShape(block.getMetadata());
                            if (shape != null) {
                                for (AABB box : shape.getBoxes()) {
                                    AABB blockBox = box.offset(x, y, z);
                                    AABB testBox = currentBox.offset(dx, 0, 0);
                                    if (testBox.intersects(blockBox)) {
                                        if (dx > 0) dx = blockBox.getMin().x - currentBox.getMax().x - 0.001f;
                                        else dx = blockBox.getMax().x - currentBox.getMin().x + 0.001f;
                                        velocity.x = 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            position.x += dx;
            currentBox = boundingBox.offset(position);
        }

        // --- Horizontal movement with collision (Z) ---
        if (dz != 0) {
            int minX = (int) Math.floor(currentBox.getMin().x);
            int maxX = (int) Math.floor(currentBox.getMax().x);
            int minZ = (int) Math.floor(currentBox.getMin().z + Math.min(0, dz));
            int maxZ = (int) Math.floor(currentBox.getMax().z + Math.max(0, dz));
            int minY = (int) Math.floor(currentBox.getMin().y);
            int maxY = (int) Math.floor(currentBox.getMax().y);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        Block block = world.getBlock(x, y, z);
                        if (!block.isAir() && block.isSolid()) {
                            com.za.minecraft.world.physics.VoxelShape shape = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType()).getShape(block.getMetadata());
                            if (shape != null) {
                                for (AABB box : shape.getBoxes()) {
                                    AABB blockBox = box.offset(x, y, z);
                                    AABB testBox = currentBox.offset(0, 0, dz);
                                    if (testBox.intersects(blockBox)) {
                                        if (dz > 0) dz = blockBox.getMin().z - currentBox.getMax().z - 0.001f;
                                        else dz = blockBox.getMax().z - currentBox.getMin().z + 0.001f;
                                        velocity.z = 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            position.z += dz;
        }
    }

    public Vector3f getPosition() { return position; }
    public Vector3f getVelocity() { return velocity; }
    public Vector3f getRotation() { return rotation; }
    public AABB getBoundingBox() { return boundingBox.offset(position); }
    public boolean isOnGround() { return onGround; }
    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }
    public void setPosition(float x, float y, float z) { position.set(x, y, z); }
}
