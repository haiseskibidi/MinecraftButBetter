package com.za.minecraft.world.blocks.entity;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.utils.Identifier;

/**
 * Сущность блока для ямного обжига (Pit Kiln).
 * Управляет процессом обжига сырой глины в течение 20 секунд.
 */
public class PitKilnBlockEntity extends BlockEntity implements ITickable {
    private float cookTime = 0;
    private final float totalTime = 20.0f; // 20 секунд
    private boolean burning = true;

    public PitKilnBlockEntity(BlockPos pos) {
        super(pos);
    }

    @Override
    public void update(float deltaTime) {
        if (!burning) return;

        cookTime += deltaTime;
        
        // Визуальные эффекты (дым и огонь) можно было бы добавить здесь, 
        // но сейчас они управляются шейдером или общими системами.
        
        if (cookTime >= totalTime) {
            finishFiring();
        }
    }

    private void finishFiring() {
        if (world != null) {
            // Get firing temperature from the block definition
            com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(world.getBlock(pos).getType());
            float temp = (def != null) ? def.getFiringTemperature() : 900.0f;

            // Drop item directly
            com.za.minecraft.world.items.ItemStack hotVessel = new com.za.minecraft.world.items.ItemStack(com.za.minecraft.world.items.Items.FIRED_VESSEL);
            hotVessel.setTemperature(temp);
            
            com.za.minecraft.entities.ItemEntity entity = new com.za.minecraft.entities.ItemEntity(
                new org.joml.Vector3f(pos.x() + 0.5f, pos.y() + 0.2f, pos.z() + 0.5f),
                hotVessel
            );
            world.spawnEntity(entity);
            
            world.setBlock(pos.x(), pos.y(), pos.z(), Blocks.AIR.getId());
            world.removeBlockEntity(pos);
        }
    }

    public float getProgress() {
        return Math.min(cookTime / totalTime, 1.0f);
    }

    public boolean isBurning() {
        return burning;
    }

    @Override
    public boolean shouldTick() {
        return burning;
    }
}
