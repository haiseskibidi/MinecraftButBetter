package com.za.minecraft.world;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.component.ToolComponent;
import com.za.minecraft.world.items.ToolType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemStackingTest {

    @Test
    public void testDefaultStackSizes() {
        Item genericItem = new Item(1, Identifier.of("test:generic"), "item.test.generic", "texture.png");
        assertEquals(16, genericItem.getMaxStackSize(), "Generic items should stack to 16 by default");

        Item toolItem = new Item(2, Identifier.of("test:tool"), "item.test.tool", "texture.png");
        toolItem.addComponent(ToolComponent.class, new ToolComponent(ToolType.PICKAXE, 2.0f, 100, false));
        assertEquals(1, toolItem.getMaxStackSize(), "Tools should stack to 1 by default");
    }

    @Test
    public void testExplicitStackSize() {
        Item customItem = new Item(3, Identifier.of("test:custom"), "item.test.custom", "texture.png");
        customItem.setMaxStackSize(64);
        assertEquals(64, customItem.getMaxStackSize(), "Explicitly set maxStackSize should be respected");

        Item nonStackable = new Item(4, Identifier.of("test:non_stackable"), "item.test.non_stackable", "texture.png");
        nonStackable.setMaxStackSize(1);
        assertEquals(1, nonStackable.getMaxStackSize(), "Explicitly set maxStackSize 1 should be respected");
    }
}
