package com.za.minecraft.world.blocks;

public class Block {
    private final BlockType type;
    public enum Axis { X, Y, Z }
    private final Axis axis;
    
    public Block(BlockType type) {
        this.type = type;
        this.axis = Axis.Y;
    }
    
    public Block(BlockType type, Axis axis) {
        this.type = type;
        this.axis = axis == null ? Axis.Y : axis;
    }
    
    public BlockType getType() {
        return type;
    }
    
    public Axis getAxis() {
        return axis;
    }
    
    public boolean isSolid() {
        return type.isSolid();
    }
    
    public boolean isTransparent() {
        return type.isTransparent();
    }
    
    public boolean isAir() {
        return type == BlockType.AIR;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Block block = (Block) obj;
        return type == block.type;
    }
    
    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
