package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.blocks.entity.ChestBlockEntity;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.engine.graphics.ui.ScreenManager;
import com.za.minecraft.engine.core.GameLoop;

/**
 * Definition for a chest block. Opens the chest GUI on use.
 */
public class ChestBlockDefinition extends BlockDefinition {
    private int inventorySize = 27; // Default 9x3

    public ChestBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    public void setInventorySize(int size) {
        this.inventorySize = size;
    }

    @Override
    public boolean hasOnUse() {
        return true;
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof ChestBlockEntity)) {
            be = new ChestBlockEntity(pos, inventorySize);
            world.setBlockEntity(be);
        }
        
        ChestBlockEntity chest = (ChestBlockEntity) be;
        int sw = GameLoop.getInstance().getWindow().getWidth();
        int sh = GameLoop.getInstance().getWindow().getHeight();
        
        GameLoop.getInstance().setInventoryOpen(true);
        GameLoop.getInstance().getInputManager().disableMouseCapture(GameLoop.getInstance().getWindow());
        ScreenManager.getInstance().openChest(chest, player.getInventory(), sw, sh);
        return true;
    }
}
