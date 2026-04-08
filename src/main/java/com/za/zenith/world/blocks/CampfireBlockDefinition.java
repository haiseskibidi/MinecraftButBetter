package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.Items;

public class CampfireBlockDefinition extends BlockDefinition {
    public CampfireBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean hasOnUse() {
        return true;
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (heldStack != null && heldStack.getItem().getId() == Items.RAW_MEAT.getId()) {
            player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), new ItemStack(Items.COOKED_MEAT));
            return true;
        }
        return false;
    }
}
