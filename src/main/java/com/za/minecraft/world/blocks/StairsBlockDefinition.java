package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.physics.VoxelShape;
import com.za.minecraft.world.physics.AABB;

public class StairsBlockDefinition extends BlockDefinition {
    
    private static final VoxelShape[] SHAPES = new VoxelShape[6];
    
    static {
        AABB bottomHalf = new AABB(0, 0, 0, 1, 0.5f, 1);
        
        // DIR_NORTH (-Z) -> high part is at North (-Z)
        SHAPES[Block.DIR_NORTH] = new VoxelShape().addBox(bottomHalf).addBox(new AABB(0, 0.5f, 0, 1, 1, 0.5f)); 
        // DIR_SOUTH (+Z) -> high part is at South (+Z)
        SHAPES[Block.DIR_SOUTH] = new VoxelShape().addBox(bottomHalf).addBox(new AABB(0, 0.5f, 0.5f, 1, 1, 1));
        // DIR_WEST (-X) -> high part is at West (-X)
        SHAPES[Block.DIR_WEST]  = new VoxelShape().addBox(bottomHalf).addBox(new AABB(0, 0.5f, 0, 0.5f, 1, 1));
        // DIR_EAST (+X) -> high part is at East (+X)
        SHAPES[Block.DIR_EAST]  = new VoxelShape().addBox(bottomHalf).addBox(new AABB(0.5f, 0.5f, 0, 1, 1, 1));
        
        SHAPES[Block.DIR_UP] = SHAPES[Block.DIR_NORTH];
        SHAPES[Block.DIR_DOWN] = SHAPES[Block.DIR_NORTH];
    }
    
    public StairsBlockDefinition(int id, String name, boolean solid, boolean transparent) {
        super(id, name, solid, transparent);
    }

    public StairsBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }
    
    @Override
    public VoxelShape getShape(byte metadata) {
        if (metadata >= 0 && metadata < SHAPES.length) {
            return SHAPES[metadata];
        }
        return SHAPES[Block.DIR_NORTH];
    }

    @Override
    public boolean isFullCube() {
        return false;
    }
}
