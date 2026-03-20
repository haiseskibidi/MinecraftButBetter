package com.za.minecraft.world.blocks;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.blocks.entity.GeneratorBlockEntity;

public class GeneratorBlockDefinition extends BlockDefinition {
    public GeneratorBlockDefinition(int id, String name, boolean solid, boolean transparent) {
        super(id, name, solid, transparent);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos) {
        return new GeneratorBlockEntity(pos);
    }
}
