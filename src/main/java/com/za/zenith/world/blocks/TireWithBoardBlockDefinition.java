package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;

public class TireWithBoardBlockDefinition extends BlockDefinition {
    public TireWithBoardBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean hasOnUse() {
        return true;
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (heldStack != null && heldStack.getItem().getIdentifier().getPath().equals("metal_sheet") && player.isSneaking()) {
            world.setBlock(pos, new Block(Blocks.SCAVENGER_TABLE.getId()));
            heldStack.split(1);
            if (heldStack.getCount() <= 0) {
                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
            }
            return true;
        }
        return false;
    }
}
