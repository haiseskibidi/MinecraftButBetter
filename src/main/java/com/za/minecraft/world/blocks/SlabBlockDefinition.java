package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.physics.VoxelShape;

public class SlabBlockDefinition extends BlockDefinition {
    
    public SlabBlockDefinition(int id, String name, boolean solid, boolean transparent) {
        super(id, name, solid, transparent);
    }
    
    public SlabBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }
    
    @Override
    public VoxelShape getShape(byte metadata) {
        if (metadata == Block.DIR_UP) {
            return VoxelShape.SLAB_TOP;
        } else if (metadata == Block.DIR_DOWN) {
            return VoxelShape.SLAB_BOTTOM;
        } else if (metadata == Block.DIR_NORTH) {
            return VoxelShape.SLAB_NORTH;
        } else if (metadata == Block.DIR_SOUTH) {
            return VoxelShape.SLAB_SOUTH;
        } else if (metadata == Block.DIR_WEST) {
            return VoxelShape.SLAB_WEST;
        } else if (metadata == Block.DIR_EAST) {
            return VoxelShape.SLAB_EAST;
        }
        return VoxelShape.SLAB_BOTTOM; // Default
    }

    @Override
    public boolean isFullCube() {
        return false;
    }
}
