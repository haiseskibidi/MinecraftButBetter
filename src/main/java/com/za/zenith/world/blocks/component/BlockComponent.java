package com.za.zenith.world.blocks.component;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;

/**
 * Базовый класс для компонентов блоков.
 * Позволяет расширять поведение блоков через композицию вместо наследования.
 */
public abstract class BlockComponent {
    
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        return false;
    }

    public boolean onLeftClick(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ, boolean isNewClick) {
        return false;
    }

    public void onPlace(World world, BlockPos pos, Player player, ItemStack heldStack) {
    }

    public void onBreak(World world, BlockPos pos, Player player) {
    }

    public void onNeighborChanged(World world, BlockPos pos, BlockPos neighborPos) {
    }

    public void onTick(World world, BlockPos pos) {
    }

    public java.util.List<com.za.zenith.world.blocks.InteractionZone> getInteractionZones(World world, BlockPos pos) {
        return java.util.Collections.emptyList();
    }

    /**
     * Позволяет компоненту добавить свои хитбоксы к основной форме блока.
     */
    public void addDynamicBoxes(World world, BlockPos pos, java.util.List<com.za.zenith.world.physics.AABB> boxes) {
    }

    /**
     * Возвращает true, если компонент перехватывает ПКМ.
     */
    public boolean hasOnUse() {
        return false;
    }
}
