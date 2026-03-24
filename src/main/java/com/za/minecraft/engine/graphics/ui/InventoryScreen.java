package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.entities.Player;
import com.za.minecraft.entities.inventory.Slot;
import com.za.minecraft.world.items.ItemStack;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all GUI windows that contain slots.
 */
public abstract class InventoryScreen implements Screen {
    protected final List<SlotUI> slots = new ArrayList<>();
    protected final String title;

    public InventoryScreen(String title) {
        this.title = title;
    }

    @Override
    public abstract void init(int screenWidth, int screenHeight);

    @Override
    public void render(UIRenderer renderer, int sw, int sh, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        for (SlotUI slotUI : slots) {
            if (slotUI.getSlot().getInventory().isSlotActive(slotUI.getSlot().getIndex())) {
                renderer.renderSlot(slotUI.getX(), slotUI.getY(), getSlotSize(), slotUI.getSlot().getStack(), slotUI.getPlaceholderTexture(), sw, sh, atlas);
            }
        }
    }

    @Override
    public boolean handleMouseClick(float mx, float my, int button) {
        // Inventory clicks are handled by InputManager for now, 
        // but we could migrate them here for consistency.
        return false;
    }

    public SlotUI getSlotAt(float mx, float my) {
        int size = getSlotSize();
        for (SlotUI slotUI : slots) {
            if (slotUI.getSlot().getInventory().isSlotActive(slotUI.getSlot().getIndex()) && slotUI.isMouseOver(mx, my, size)) {
                return slotUI;
            }
        }
        return null;
    }

    protected int getSlotSize() {
        return (int)(18 * Hotbar.HOTBAR_SCALE);
    }

    protected int getSpacing() {
        return (int)(2 * Hotbar.HOTBAR_SCALE);
    }
    
    public List<SlotUI> getSlots() {
        return slots;
    }
}
