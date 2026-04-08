package com.za.zenith.engine.graphics.ui;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.entities.Player;
import com.za.zenith.entities.inventory.Slot;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.utils.Identifier;
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
    public void render(UIRenderer renderer, int sw, int sh, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        float mx = com.za.zenith.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        float my = com.za.zenith.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().y;
        int size = getSlotSize();

        for (SlotUI slotUI : slots) {
            if (slotUI.getSlot().getInventory().isSlotActive(slotUI.getSlot().getIndex())) {
                boolean isHovered = slotUI.isMouseOver(mx, my, size);
                String animId = "slot_" + slotUI.getSlot().getIndex();
                renderer.renderSlot(slotUI.getX(), slotUI.getY(), size, slotUI.getSlot().getStack(), slotUI.getPlaceholderTexture(), sw, sh, atlas, isHovered, animId, true);
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

    /**
     * Handles Shift+Click logic for this screen.
     */
    public void onQuickMove(SlotUI slotUI, com.za.zenith.entities.Player player) {
        handleQuickMove(slotUI, player);
    }

    private void handleQuickMove(SlotUI originUI, Player player) {
        Slot originSlot = originUI.getSlot();
        ItemStack stack = originSlot.getStack();
        if (stack == null) return;

        // Get the screen configuration
        Identifier screenId = getScreenIdentifier();
        GUIConfig config = GUIRegistry.get(screenId);
        if (config == null) return;

        // Find the group config for the origin slot
        GUIConfig.GroupConfig originGroupCfg = null;
        for (GUIConfig.GroupConfig g : config.groups) {
            if (g.id.equals(originUI.getGroupId())) {
                originGroupCfg = g;
                break;
            }
        }
        if (originGroupCfg == null || originGroupCfg.quickMoveTo == null) return;

        // Target groups in priority order
        for (String targetGroupId : originGroupCfg.quickMoveTo) {
            // Find all slots belonging to this target group
            List<Slot> targetSlots = new ArrayList<>();
            for (SlotUI ui : slots) {
                if (ui.getGroupId().equals(targetGroupId)) {
                    targetSlots.add(ui.getSlot());
                }
            }

            // 1. Try to stack
            for (Slot target : targetSlots) {
                ItemStack targetStack = target.getStack();
                if (targetStack != null && targetStack.isStackableWith(stack) && !targetStack.isFull()) {
                    int space = targetStack.getAvailableSpace();
                    int toMove = Math.min(space, stack.getCount());
                    targetStack.setCount(targetStack.getCount() + toMove);
                    stack.setCount(stack.getCount() - toMove);
                    if (stack.getCount() <= 0) {
                        originSlot.setStack(null);
                        return;
                    }
                }
            }

            // 2. Try empty slots
            for (Slot target : targetSlots) {
                if (target.getStack() == null && target.isItemValid(stack)) {
                    target.setStack(stack.copy());
                    stack.setCount(0);
                    originSlot.setStack(null);
                    return;
                }
            }
        }
    }

    protected abstract Identifier getScreenIdentifier();

    protected int getSlotSize() {
        return (int)(18 * Hotbar.HOTBAR_SCALE);
    }

    protected int getSpacing() {
        return (int)(2 * Hotbar.HOTBAR_SCALE);
    }
    
    public List<SlotUI> getSlots() {
        return slots;
    }

    public String getTitle() {
        return title;
    }
}


