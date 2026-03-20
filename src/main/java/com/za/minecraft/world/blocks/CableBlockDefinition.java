package com.za.minecraft.world.blocks;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.blocks.entity.CableBlockEntity;

public class CableBlockDefinition extends BlockDefinition {
    public CableBlockDefinition(int id, String name, boolean solid, boolean transparent) {
        super(id, name, solid, transparent);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos) {
        return new CableBlockEntity(pos);
    }
}
