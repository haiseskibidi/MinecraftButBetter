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
            // Превращаем в обожженный сосуд
            // В будущем здесь будет проверка рецепта, пока хардкодим fired_vessel
            world.setBlock(pos.x(), pos.y(), pos.z(), Blocks.FIRED_VESSEL.getId());
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
