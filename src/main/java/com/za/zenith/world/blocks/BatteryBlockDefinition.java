package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.entity.BatteryBlockEntity;
import com.za.zenith.world.blocks.entity.BlockEntity;

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
