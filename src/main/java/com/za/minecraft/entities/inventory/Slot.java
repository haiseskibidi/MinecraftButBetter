package com.za.minecraft.entities.inventory;

import com.za.minecraft.world.inventory.IInventory;
import com.za.minecraft.world.items.ItemStack;
import java.util.function.Predicate;

public class Slot {
    private final IInventory inventory;
    private final int index;
    private final String type;
    private Predicate<ItemStack> validator;
    private String placeholderTexture;

    public Slot(IInventory inventory, int index, String type) {
        this.inventory = inventory;
        this.index = index;
        this.type = type;
        this.validator = (stack) -> true;
    }

    public Slot withValidator(Predicate<ItemStack> validator) {
        this.validator = validator;
        return this;
    }

    public Slot withPlaceholder(String texture) {
        this.placeholderTexture = texture;
        return this;
    }

    public String getPlaceholderTexture() {
        return placeholderTexture;
    }

    public IInventory getInventory() {
        return inventory;
    }

    public int getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }

    public ItemStack getStack() {
        return inventory.getStack(index);
    }

    public void setStack(ItemStack stack) {
        inventory.setStack(index, stack);
    }

    public boolean isItemValid(ItemStack stack) {
        if (stack == null) return true;
        
        // Strict equipment check: if item has strict equipment component, it MUST match slot type
        com.za.minecraft.world.items.component.EquipmentComponent eq = stack.getItem().getComponent(com.za.minecraft.world.items.component.EquipmentComponent.class);
        if (eq != null && eq.isStrict()) {
            if (!eq.getSlotType().equals(this.type)) return false;
        }

        return validator.test(stack) && inventory.isItemValid(index, stack);
    }
}
