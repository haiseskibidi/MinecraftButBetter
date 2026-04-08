package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.component.FuelComponent;
import com.za.zenith.world.blocks.entity.GeneratorBlockEntity;

public class GeneratorBlockDefinition extends BlockDefinition {
    public GeneratorBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        com.za.zenith.world.blocks.entity.BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof GeneratorBlockEntity generator) {
            if (heldStack != null && heldStack.getItem().hasComponent(FuelComponent.class)) {
                FuelComponent fuel = heldStack.getItem().getComponent(FuelComponent.class);
                generator.addFuel(fuel.fuelAmount());
                
                ItemStack newStack = heldStack.getCount() > 1 ? new ItemStack(heldStack.getItem(), heldStack.getCount() - 1) : null;
                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), newStack);
                return true;
            }
            return true; // Consume action even if no fuel to prevent block placement
        }
        return false;
    }
}
