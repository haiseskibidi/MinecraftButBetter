package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.Items;

public class UnfiredVesselBlockDefinition extends BlockDefinition {
    public UnfiredVesselBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean hasOnUse() {
        return true;
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (player.isSneaking() && heldStack != null && heldStack.getItem().getId() == Items.STRAW.getId()) {
            world.setBlock(pos, new Block(Blocks.PIT_KILN.getId(), (byte)0));
            player.getInventory().consumeSelected(1);
            return true;
        }
        return false;
    }
}


