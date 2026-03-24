package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.Items;
import com.za.minecraft.entities.DecorationEntity;
import org.joml.Vector3f;

public class PitKilnBlockDefinition extends BlockDefinition {
    public PitKilnBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) return false;

        Block block = world.getBlock(pos);
        int logs = block.getMetadata();
        
        if (heldStack == null) return false;

        // 1. Adding Logs
        if (logs < 4 && heldStack.getItem().getIdentifier().getPath().contains("log")) {
            float[][] offsets = {{0.25f, 0, 0}, {-0.25f, 0, 0}, {0, 0, 0.25f}, {0, 0, -0.25f}};
            float[] rotations = {0, 0, 1.57f, 1.57f};
            float yOffset = (logs >= 2) ? 0.005f : 0.0f;
            
            Vector3f decoPos = new Vector3f(pos.x() + 0.5f + offsets[logs][0], pos.y() + 0.05f + yOffset, pos.z() + 0.5f + offsets[logs][2]);
            world.spawnEntity(new DecorationEntity(decoPos, Identifier.of("minecraft:log_pile"), rotations[logs]));
            
            world.setBlock(pos, new Block(getId(), (byte)(logs + 1)));
            player.getInventory().consumeSelected(1);
            return true;
        } 
        
        // 2. Lighting the fire
        if (logs == 4 && heldStack.getItem().getId() == Items.FIRE_STARTER.getId()) {
            // Remove log pile entities
            world.getEntities().stream()
                .filter(e -> e instanceof DecorationEntity)
                .filter(e -> e.getPosition().distance(new Vector3f(pos.x() + 0.5f, pos.y(), pos.z() + 0.5f)) < 1.0f)
                .forEach(e -> e.setRemoved());
            
            world.setBlock(pos, new Block(Blocks.BURNING_PIT_KILN.getId()));
            world.setBlockEntity(new com.za.minecraft.world.blocks.entity.PitKilnBlockEntity(pos));
            // Durability reduction can be added here
            return true;
        }

        return false;
    }
}
