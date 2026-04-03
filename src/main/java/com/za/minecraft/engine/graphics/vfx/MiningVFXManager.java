package com.za.minecraft.engine.graphics.vfx;

import com.za.minecraft.entities.Player;
import com.za.minecraft.engine.input.MiningController;
import com.za.minecraft.world.items.ItemStack;

/**
 * Manages visual effects related to mining, specifically the "Heat" of the tool.
 * Heat now decays naturally even if the item in hand changes.
 */
public class MiningVFXManager {
    private float heatLevel = 0.0f;
    
    // Tracking current item state for logic resets
    private int lastItemIdentity = -1;
    private int lastSlotIndex = -1;
    private int lastItemCount = -1;

    public void update(float deltaTime, Player player, MiningController mining) {
        if (player == null || mining == null) return;

        int currentSlot = player.getInventory().getSelectedSlot();
        ItemStack currentStack = player.getInventory().getSelectedItemStack();
        
        int currentIdentity = (currentStack != null) ? System.identityHashCode(currentStack) : 0;
        int currentCount = (currentStack != null) ? currentStack.getCount() : 0;

        // --- PROGRESS RESET LOGIC ---
        // If identity changes OR slot changes OR count drops, we reset MINING progress.
        // But we DO NOT reset visual heatLevel to 0 immediately anymore.
        if (currentIdentity != lastItemIdentity || currentSlot != lastSlotIndex || currentCount < lastItemCount) {
            if (mining.getBreakingBlockPos() != null) {
                mining.reset();
            }
        }

        lastItemIdentity = currentIdentity;
        lastSlotIndex = currentSlot;
        lastItemCount = currentCount;

        // --- HEAT ACCUMULATION & DECAY ---
        float targetHeat = 0.0f;
        if (mining.getBreakingBlockPos() != null) {
            targetHeat = mining.getBreakingProgress();
        }

        if (heatLevel < targetHeat) {
            // Heat builds up fast during action
            heatLevel = Math.min(targetHeat, heatLevel + deltaTime * 2.0f);
        } else {
            // Heat always decays naturally (approx 2 seconds), even if tool was swapped
            heatLevel = Math.max(targetHeat, heatLevel - deltaTime * 0.5f);
        }
    }

    public float getHeatLevel() {
        return heatLevel;
    }
}
