package com.za.minecraft.entities;

import com.za.minecraft.world.items.ItemStack;
import org.joml.Vector3f;

/**
 * Статичная сущность ресурса, лежащего на земле (палка, камень).
 * Не имеет логики обновления, только данные для рендеринга.
 */
public class ResourceEntity extends Entity {
    private final ItemStack stack;
    private final float rotation;

    public ResourceEntity(Vector3f position, ItemStack stack, float rotation) {
        super(position, 0.5f, 0.1f);
        this.stack = stack;
        this.rotation = rotation;
        this.getRotation().y = rotation;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void update(float delta, com.za.minecraft.world.World world) {
        // Статичные ресурсы не обновляются для экономии CPU
    }
}
