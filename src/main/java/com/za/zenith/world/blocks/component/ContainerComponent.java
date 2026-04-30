package com.za.zenith.world.blocks.component;

import com.google.gson.annotations.SerializedName;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.blocks.entity.ModularBlockEntity;
import com.za.zenith.engine.graphics.ui.ScreenManager;
import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.utils.Identifier;

/**
 * Компонент инвентаря для блоков (сундуки и т.д.).
 */
public class ContainerComponent extends BlockComponent implements InventoryProvider {
    @SerializedName("inventory_size")
    private int inventorySize = 27;
    @SerializedName("gui_id")
    private String guiId = "zenith:chest";

    @Override
    public int getRequiredInventorySize() {
        return inventorySize;
    }

    @Override
    public boolean hasOnUse() {
        return true;
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;

        var be = world.getBlockEntity(pos);
        if (!(be instanceof ModularBlockEntity modular)) return false;

        int sw = GameLoop.getInstance().getWindow().getWidth();
        int sh = GameLoop.getInstance().getWindow().getHeight();
        
        GameLoop.getInstance().setInventoryOpen(true);
        GameLoop.getInstance().getInputManager().disableMouseCapture(GameLoop.getInstance().getWindow());
        ScreenManager.getInstance().openChest(modular, player, Identifier.of(guiId), sw, sh);
        return true;
    }
}
