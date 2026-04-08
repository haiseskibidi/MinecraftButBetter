package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.entity.BlockEntity;
import com.za.zenith.world.blocks.entity.LampBlockEntity;

public class LampBlockDefinition extends BlockDefinition {
    public LampBlockDefinition(int id, String name, boolean solid, boolean transparent) {
        super(id, name, solid, transparent);
    }

    public LampBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos) {
        return new LampBlockEntity(pos);
    }
}
