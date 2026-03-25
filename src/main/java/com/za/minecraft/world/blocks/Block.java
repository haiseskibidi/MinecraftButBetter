package com.za.minecraft.world.blocks;

import com.za.minecraft.world.physics.VoxelShape;

public class Block {
    private final int type;
    private final byte metadata;
    
    // Metadata constants for directions
    public static final byte DIR_UP = 0;
    public static final byte DIR_DOWN = 1;
    public static final byte DIR_NORTH = 2; // -Z
    public static final byte DIR_SOUTH = 3; // +Z
    public static final byte DIR_WEST = 4;  // -X
    public static final byte DIR_EAST = 5;  // +X
    
    // Flags
    public static final byte BIT_NATURAL = (byte)0x80; // 1000 0000
    
    // Legacy axes (can be mapped to metadata)
    public static final byte AXIS_Y = DIR_UP;
    public static final byte AXIS_X = DIR_EAST;
    public static final byte AXIS_Z = DIR_SOUTH;
    
    public Block(int type) {
        this.type = type;
        this.metadata = 0;
    }
    
    public Block(int type, byte metadata) {
        this.type = type;
        this.metadata = metadata;
    }
    
    public int getType() {
        return type;
    }
    
    public byte getMetadata() {
        return metadata;
    }

    public boolean isNatural() {
        return (metadata & BIT_NATURAL) != 0;
    }

    public byte getDirection() {
        return (byte)(metadata & 0x0F);
    }
    
    public boolean isSolid() {
        return BlockRegistry.getBlock(type).isSolid();
    }

    public boolean isReplaceable() {
        return BlockRegistry.getBlock(type).isReplaceable();
    }

    public boolean isTransparent() {
        return BlockRegistry.getBlock(type).isTransparent();
    }
    
    public boolean isFullCube() {
        return BlockRegistry.getBlock(type).isFullCube();
    }
    
    public boolean isAir() {
        return type == Blocks.AIR.getId();
    }
    
    public VoxelShape getShape() {
        return BlockRegistry.getBlock(type).getShape(metadata);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Block block = (Block) obj;
        return type == block.type && metadata == block.metadata;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(type) * 31 + Byte.hashCode(metadata);
    }
}
