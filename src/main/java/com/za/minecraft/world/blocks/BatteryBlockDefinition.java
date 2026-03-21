package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.entity.BatteryBlockEntity;
import com.za.minecraft.world.blocks.entity.BlockEntity;

public class BatteryBlockDefinition extends BlockDefinition {
    public BatteryBlockDefinition(int id, String name, boolean solid, boolean transparent) {
        super(id, name, solid, transparent);
    }

    public BatteryBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos) {
        return new BatteryBlockEntity(pos);
    }
}
