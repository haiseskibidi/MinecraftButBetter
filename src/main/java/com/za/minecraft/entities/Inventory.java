package com.za.minecraft.entities;

import com.za.minecraft.world.inventory.IInventory;
import com.za.minecraft.entities.inventory.Slot;
import com.za.minecraft.entities.inventory.SlotGroup;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.Items;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.world.World;
import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.world.items.component.BagComponent;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class Inventory implements IInventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int POCKETS_SIZE = 8;
    public static final int EQUIPMENT_SIZE = 8; // Reserved space for equipment
    public static final int POUCH_SIZE = 8;
    public static final int TOTAL_SIZE = 33; // 9 (hotbar) + 8 (pockets) + 8 (equipment) + 8 (pouch)

    public static final int START_HOTBAR = 0;
    public static final int START_POCKETS = 9;
    public static final int START_EQUIPMENT = 17;
    public static final int START_POUCH = 25;
    
    public static final int SLOT_HELMET = 17;
    public static final int SLOT_CHEST = 18;
    public static final int SLOT_LEGS = 19;
    public static final int SLOT_BOOTS = 20;
    public static final int SLOT_OFFHAND = 21;
    public static final int SLOT_ACCESSORY = 22;
    
    private ItemStack[] slots;
    private int selectedSlot; 
    private final List<SlotGroup> groups;
    
    public Inventory() {
        this.slots = new ItemStack[TOTAL_SIZE];
        this.selectedSlot = 0;
        this.groups = new ArrayList<>();
        
        initGroups();
        
        // Initial items in hotbar (slots 0-8)
        slots[0] = new ItemStack(Items.STONE_KNIFE);
        slots[1] = new ItemStack(Items.SCRAP_PICKAXE);
        slots[2] = new ItemStack(Items.CROWBAR);
        slots[3] = new ItemStack(ItemRegistry.getItem(Blocks.STONE.getId()));
        slots[4] = new ItemStack(ItemRegistry.getItem(Blocks.OAK_LOG.getId()));
        slots[5] = new ItemStack(ItemRegistry.getItem(Blocks.OAK_PLANKS.getId()));
        slots[6] = new ItemStack(ItemRegistry.getItem(Blocks.COBBLESTONE.getId()));
        slots[7] = new ItemStack(ItemRegistry.getItem(Blocks.RUSTY_METAL.getId()));
        slots[8] = new ItemStack(ItemRegistry.getItem(Blocks.ASPHALT.getId()));
    }

    private void initGroups() {
        // 1. Hotbar
        SlotGroup hotbar = new SlotGroup("hotbar");
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            hotbar.addSlot(new Slot(this, START_HOTBAR + i, "any"));
        }
        groups.add(hotbar);

        // 2. Pockets
        SlotGroup pockets = new SlotGroup("pockets");
        for (int i = 0; i < POCKETS_SIZE; i++) {
            pockets.addSlot(new Slot(this, START_POCKETS + i, "any"));
        }
        groups.add(pockets);

        // 3. Equipment
        SlotGroup equipment = new SlotGroup("equipment");
        equipment.addSlot(new Slot(this, SLOT_HELMET, "armor_head"));
        equipment.addSlot(new Slot(this, SLOT_CHEST, "armor_chest"));
        equipment.addSlot(new Slot(this, SLOT_LEGS, "armor_legs"));
        equipment.addSlot(new Slot(this, SLOT_BOOTS, "armor_boots"));
        equipment.addSlot(new Slot(this, SLOT_OFFHAND, "offhand"));
        equipment.addSlot(new Slot(this, SLOT_ACCESSORY, "accessory")
            .withValidator(stack -> stack.getItem().hasComponent(BagComponent.class)));
        groups.add(equipment);

        // 4. Pouch
        SlotGroup pouch = new SlotGroup("pouch")
            .withActiveSupplier(() -> {
                ItemStack acc = slots[SLOT_ACCESSORY];
                return acc != null && acc.getItem().hasComponent(BagComponent.class);
            });
        for (int i = 0; i < POUCH_SIZE; i++) {
            pouch.addSlot(new Slot(this, START_POUCH + i, "any"));
        }
        groups.add(pouch);
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < TOTAL_SIZE) return slots[slot];
        return null;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < TOTAL_SIZE) slots[slot] = stack;
    }

    @Override
    public int size() {
        return TOTAL_SIZE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true; // Slots themselves handle more specific validation
    }

    public List<SlotGroup> getGroups() {
        return groups;
    }

    public SlotGroup getGroup(String id) {
        return groups.stream().filter(g -> g.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public boolean isSlotActive(int slotIndex) {
        for (SlotGroup group : groups) {
            for (Slot slot : group.getSlots()) {
                if (slot.getIndex() == slotIndex) {
                    return group.isActive();
                }
            }
        }
        return true;
    }
    
    public ItemStack getSelectedItemStack() {
        return slots[selectedSlot];
    }
    
    public Item getSelectedItem() {
        ItemStack stack = getSelectedItemStack();
        return stack != null ? stack.getItem() : null;
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            this.selectedSlot = slot;
        }
    }

    public void consumeSelected(int amount) {
        consume(selectedSlot, amount);
    }
    
    public ItemStack getStackInSlot(int slot) {
        return getStack(slot);
    }
    
    public void setStackInSlot(int slot, ItemStack stack) {
        setStack(slot, stack);
    }

    public boolean addItem(ItemStack stack) {
        if (stack == null) return false;
        
        // 1. Try to stack in active groups (skipping equipment by default for auto-add)
        for (SlotGroup group : groups) {
            if (!group.isActive() || group.getId().equals("equipment")) continue;
            
            for (Slot slot : group.getSlots()) {
                ItemStack existing = slot.getStack();
                if (existing != null && existing.isStackableWith(stack)) {
                    existing.setCount(existing.getCount() + stack.getCount());
                    return true;
                }
            }
        }
        
        // 2. Try to find empty in active groups
        for (SlotGroup group : groups) {
            if (!group.isActive() || group.getId().equals("equipment")) continue;
            
            for (Slot slot : group.getSlots()) {
                if (slot.getStack() == null && slot.isItemValid(stack)) {
                    slot.setStack(stack);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void nextSlot() {
        selectedSlot = (selectedSlot + 1) % HOTBAR_SIZE;
        logSelection();
    }
    
    public void previousSlot() {
        selectedSlot = (selectedSlot - 1 + HOTBAR_SIZE) % HOTBAR_SIZE;
        logSelection();
    }
    
    public void swapSlots(int slotA, int slotB) {
        if (slotA >= 0 && slotA < TOTAL_SIZE && slotB >= 0 && slotB < TOTAL_SIZE) {
            ItemStack temp = slots[slotA];
            slots[slotA] = slots[slotB];
            slots[slotB] = temp;
        }
    }

    public void quickMove(int slotIndex) {
        ItemStack stack = getStackInSlot(slotIndex);
        if (stack == null) return;

        // Определяем целевые группы для квик-мува
        List<String> targetGroups = new ArrayList<>();
        if (slotIndex < HOTBAR_SIZE) {
            targetGroups.add("pockets");
            targetGroups.add("pouch");
        } else {
            targetGroups.add("hotbar");
        }

        // 1. Try to stack
        for (String groupId : targetGroups) {
            SlotGroup group = getGroup(groupId);
            if (group == null || !group.isActive()) continue;
            
            for (Slot slot : group.getSlots()) {
                ItemStack existing = slot.getStack();
                if (existing != null && existing.isStackableWith(stack)) {
                    existing.setCount(existing.getCount() + stack.getCount());
                    setStackInSlot(slotIndex, null);
                    return;
                }
            }
        }

        // 2. Try to find empty slot
        for (String groupId : targetGroups) {
            SlotGroup group = getGroup(groupId);
            if (group == null || !group.isActive()) continue;
            
            for (Slot slot : group.getSlots()) {
                if (slot.getStack() == null && slot.isItemValid(stack)) {
                    slot.setStack(stack);
                    setStackInSlot(slotIndex, null);
                    return;
                }
            }
        }
    }
    
    public void sortMainInventory() {
        List<Slot> validSlots = new ArrayList<>();
        for (SlotGroup group : groups) {
            if (group.isActive() && (group.getId().equals("pockets") || group.getId().equals("pouch"))) {
                validSlots.addAll(group.getSlots());
            }
        }

        // Combining stacks
        for (int i = 0; i < validSlots.size(); i++) {
            Slot slotI = validSlots.get(i);
            ItemStack stackI = slotI.getStack();
            if (stackI != null) {
                for (int j = i + 1; j < validSlots.size(); j++) {
                    Slot slotJ = validSlots.get(j);
                    ItemStack stackJ = slotJ.getStack();
                    if (stackJ != null && stackI.isStackableWith(stackJ)) {
                        stackI.setCount(stackI.getCount() + stackJ.getCount());
                        slotJ.setStack(null);
                    }
                }
            }
        }

        // Sorting by ID and bubbling nulls
        for (int i = 0; i < validSlots.size(); i++) {
            Slot slotI = validSlots.get(i);
            for (int j = i + 1; j < validSlots.size(); j++) {
                Slot slotJ = validSlots.get(j);
                
                ItemStack stackI = slotI.getStack();
                ItemStack stackJ = slotJ.getStack();
                
                boolean swap = false;
                if (stackI == null && stackJ != null) swap = true;
                else if (stackI != null && stackJ != null) {
                    if (stackI.getItem().getId() > stackJ.getItem().getId()) swap = true;
                }
                
                if (swap) {
                    slotI.setStack(stackJ);
                    slotJ.setStack(stackI);
                }
            }
        }
    }
    
    public void update(World world, Player player, Camera camera) {
        // Проверяем каждую группу. Если она неактивна, но в её слотах есть предметы - выбрасываем их.
        for (SlotGroup group : groups) {
            if (!group.isActive()) {
                for (Slot slot : group.getSlots()) {
                    ItemStack stack = slots[slot.getIndex()];
                    if (stack != null) {
                        dropStack(stack, player, world, camera, true);
                        slots[slot.getIndex()] = null;
                    }
                }
            }
        }
    }

    private void dropStack(ItemStack stack, Player player, World world, Camera camera, boolean fullStack) {
        Vector3f lookDir = new Vector3f(0, 0, -1).rotateX(camera.getRotation().x).rotateY(camera.getRotation().y).normalize();
        Vector3f pos = new Vector3f(player.getPosition()).add(0, 1.5f, 0).add(new Vector3f(lookDir).mul(0.5f));
        
        ItemStack toDrop = fullStack ? stack.copy() : stack.split(1);
        if (fullStack) stack.setCount(0);
        
        com.za.minecraft.entities.ItemEntity entity = new com.za.minecraft.entities.ItemEntity(pos, toDrop);
        entity.getVelocity().set(lookDir.x * 0.3f, lookDir.y * 0.3f + 0.1f, lookDir.z * 0.3f);
        world.spawnEntity(entity);
    }

    private void logSelection() {
        Item current = getSelectedItem();
        String name = (current != null) ? current.getName() : "EMPTY";
        com.za.minecraft.utils.Logger.info("Selected slot %d: %s", selectedSlot, name);
    }
}
