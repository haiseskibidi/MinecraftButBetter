package com.za.zenith.engine.graphics.vfx;

import com.za.zenith.entities.Player;
import com.za.zenith.engine.input.MiningController;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.Items;

/**
 * Manages visual effects related to mining.
 * Separates heat for hands and items to prevent visual transfer bugs.
 */
public class MiningVFXManager {
    private float handHeat = 0.0f;
    private float itemHeat = 0.0f;
    
    private int lastItemIdentity = -1;
    private int lastSlotIndex = -1;
    private int lastItemCount = -1;

    public void update(float deltaTime, Player player, MiningController mining) {
        if (player == null || mining == null) return;

        int currentSlot = player.getInventory().getSelectedSlot();
        ItemStack currentStack = player.getInventory().getSelectedItemStack();
        
        int currentIdentity = (currentStack != null) ? System.identityHashCode(currentStack) : 0;
        int currentCount = (currentStack != null) ? currentStack.getCount() : 0;
        boolean hasItem = (currentStack != null && currentStack.getItem() != Items.HAND);

        // --- RESET LOGIC ---
        if (currentIdentity != lastItemIdentity || currentSlot != lastSlotIndex || currentCount < lastItemCount) {
            // New item in hand is always cold
            itemHeat = 0.0f;
            if (mining.getBreakingBlockPos() != null) {
                mining.reset();
            }
        }

        lastItemIdentity = currentIdentity;
        lastSlotIndex = currentSlot;
        lastItemCount = currentCount;

        // --- TARGET SEPARATION ---
        float progress = (mining.getBreakingBlockPos() != null) ? mining.getBreakingProgress() : 0.0f;
        float targetHand = (!hasItem) ? progress : 0.0f;
        float targetItem = (hasItem) ? progress : 0.0f;

        // Update Hand Heat (Natural decay)
        if (handHeat < targetHand) {
            handHeat = Math.min(targetHand, handHeat + deltaTime * 2.0f);
        } else {
            handHeat = Math.max(targetHand, handHeat - deltaTime * 0.5f);
        }

        // Update Item Heat (Natural decay)
        if (itemHeat < targetItem) {
            itemHeat = Math.min(targetItem, itemHeat + deltaTime * 2.0f);
        } else {
            itemHeat = Math.max(targetItem, itemHeat - deltaTime * 0.5f);
        }
    }

    public float getHandHeat() { return handHeat; }
    public float getItemHeat() { return itemHeat; }
}


