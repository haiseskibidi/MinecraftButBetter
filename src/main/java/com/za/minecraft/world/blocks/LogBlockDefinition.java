package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.Items;

public class LogBlockDefinition extends BlockDefinition {
    public LogBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (heldStack != null && heldStack.getItem().getId() == Items.STONE_AXE.getId()) {
            world.setBlock(pos, new Block(Blocks.STUMP.getId()));
            com.za.minecraft.utils.Logger.info("Created a stump from log");
            return true;
        }
        return false;
    }
}
