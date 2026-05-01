package com.za.zenith.entities;

import com.za.zenith.world.inventory.IInventory;
import com.za.zenith.entities.inventory.Slot;
import com.za.zenith.entities.inventory.SlotGroup;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.Items;
import com.za.zenith.world.items.ItemRegistry;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.World;
import com.za.zenith.engine.graphics.Camera;
import com.za.zenith.world.items.component.BagComponent;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class Inventory implements IInventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int POCKETS_SIZE = 8;
    public static final int EQUIPMENT_SIZE = 8; // Reserved space for equipment
    public static final int TOTAL_SIZE = 25; // 9 (hotbar) + 8 (pockets) + 8 (equipment)

    public static final int START_HOTBAR = 0;
    public static final int START_POCKETS = 9;
    public static final int START_EQUIPMENT = 17;
    
    public static final int SLOT_HELMET = 17;
    public static final int SLOT_CHEST = 18;
    public static final int SLOT_LEGS = 19;
    public static final int SLOT_BOOTS = 20;
    public static final int SLOT_OFFHAND = 21;
    public static final int SLOT_ACCESSORY = 22;
    
    private ItemStack[] slots;
    private int selectedSlot; 
    private final List<SlotGroup> groups;
    private ItemStack lastAccessory;
    
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
            .withValidator(stack -> {
                com.za.zenith.world.items.component.EquipmentComponent eq = stack.getItem().getComponent(com.za.zenith.world.items.component.EquipmentComponent.class);
                return eq != null && eq.getSlotType().equals("accessory");
            }));
        groups.add(equipment);

        // 4. Pouch
        SlotGroup pouch = new SlotGroup("pouch")
            .withActiveSupplier(() -> {
                ItemStack acc = slots[SLOT_ACCESSORY];
                return acc != null && acc.getItem().hasComponent(BagComponent.class);
            });
        groups.add(pouch);
    }

    public boolean isFull() {
        for (SlotGroup group : groups) {
            if (!group.isActive() || group.getId().equals("equipment")) continue;
            for (com.za.zenith.entities.inventory.Slot slot : group.getSlots()) {
                if (slot.getStack() == null) return false;
            }
        }
        return true;
    }

    public <T extends com.za.zenith.world.items.component.ItemComponent> T getActiveComponent(Class<T> componentClass) {
        ItemStack selected = getSelectedItemStack();
        if (selected != null) {
            T comp = selected.getItem().getComponent(componentClass);
            if (comp != null) return comp;
        }

        SlotGroup equipment = getGroup("equipment");
        if (equipment != null) {
            for (Slot slot : equipment.getSlots()) {
                if (slot.getStack() != null) {
                    T comp = slot.getStack().getItem().getComponent(componentClass);
                    if (comp != null) return comp;
                }
            }
        }
        return null;
    }

    public boolean hasActiveComponent(Class<? extends com.za.zenith.world.items.component.ItemComponent> componentClass) {
        SlotGroup equipment = getGroup("equipment");
        if (equipment == null) return false;
        
        for (Slot slot : equipment.getSlots()) {
            ItemStack stack = slot.getStack();
            if (stack != null && stack.getItem().hasComponent(componentClass)) {
                return true;
            }
        }
        return false;
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
                if (slot.getIndex() == slotIndex && slot.getInventory() == this) {
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
        return addItem(stack, false);
    }

    public boolean addItem(ItemStack stack, boolean isFromWorld) {
        if (stack == null || stack.getCount() <= 0) return false;
        
        int originalCount = stack.getCount();

        // 1. Try to stack in active groups (skipping equipment by default for auto-add)
        for (SlotGroup group : groups) {
            if (!group.isActive() || group.getId().equals("equipment")) continue;
            
            for (com.za.zenith.entities.inventory.Slot slot : group.getSlots()) {
                ItemStack existing = slot.getStack();
                if (existing != null && existing.getItem().getId() == stack.getItem().getId() && !existing.isFull()) {
                    int space = existing.getAvailableSpace();
                    int toAdd = Math.min(space, stack.getCount());
                    
                    existing.setCount(existing.getCount() + toAdd);
                    stack.setCount(stack.getCount() - toAdd);
                    
                    if (stack.getCount() <= 0) break;
                }
            }
            if (stack.getCount() <= 0) break;
        }
        
        // 2. Try to find empty in active groups
        if (stack.getCount() > 0) {
            for (SlotGroup group : groups) {
                if (!group.isActive() || group.getId().equals("equipment")) continue;
                
                for (com.za.zenith.entities.inventory.Slot slot : group.getSlots()) {
                    if (slot.getStack() == null && slot.isItemValid(stack)) {
                        slot.setStack(stack.copy());
                        stack.setCount(0);
                        break;
                    }
                }
                if (stack.getCount() <= 0) break;
            }
        }

        int addedCount = originalCount - stack.getCount();
        if (addedCount > 0) {
            if (isFromWorld) {
                ItemStack notifyStack = stack.copy();
                notifyStack.setCount(addedCount);
                com.za.zenith.engine.graphics.ui.NotificationManager.getInstance().pushPickup(notifyStack);
                
                if (isFull() && stack.getCount() > 0) {
                    com.za.zenith.engine.graphics.ui.NotificationTriggers.getInstance().onInventoryFull();
                }
            }
            return true;
        } else if (isFromWorld) {
            com.za.zenith.engine.graphics.ui.NotificationTriggers.getInstance().onInventoryFull();
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

    public void swapWithHotbar(Slot slot, int hotbarIndex) {
        if (slot == null || hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE) return;
        
        ItemStack temp = slot.getStack();
        slot.setStack(slots[hotbarIndex]);
        slots[hotbarIndex] = temp;
    }

    public ItemStack getEquippedItem(String slotType) {
        SlotGroup equipment = getGroup("equipment");
        if (equipment == null) return null;
        for (Slot slot : equipment.getSlots()) {
            if (slot.getType().equals(slotType)) {
                return slot.getStack();
            }
        }
        return null;
    }

    public void copyFromDevPanel(Item item, int hotbarIndex) {
        if (hotbarIndex < 0 || hotbarIndex >= HOTBAR_SIZE) return;
        slots[hotbarIndex] = new ItemStack(item, item.getMaxStackSize());
    }

    public void collectAllTo(Slot targetSlot) {
        if (targetSlot == null) return;
        ItemStack targetStack = targetSlot.getStack();
        if (targetStack == null || targetStack.isFull()) return;

        Item itemType = targetStack.getItem();
        int maxStack = itemType.getMaxStackSize();

        for (SlotGroup group : groups) {
            if (!group.isActive() || group.getId().equals("equipment")) continue;

            for (com.za.zenith.entities.inventory.Slot slot : group.getSlots()) {
                if (slot == targetSlot) continue;

                ItemStack otherStack = slot.getStack();
                if (otherStack != null && otherStack.getItem().getId() == itemType.getId()) {
                    int space = maxStack - targetStack.getCount();
                    int toTake = Math.min(space, otherStack.getCount());

                    targetStack.setCount(targetStack.getCount() + toTake);
                    otherStack.setCount(otherStack.getCount() - toTake);

                    if (otherStack.getCount() <= 0) {
                        slot.setStack(null);
                    }

                    if (targetStack.isFull()) return;
                }
            }
        }
    }

    public void quickMove(Slot originSlot) {
        if (originSlot == null) return;
        ItemStack stack = originSlot.getStack();
        if (stack == null) return;

        com.za.zenith.world.items.component.EquipmentComponent eq = stack.getItem().getComponent(com.za.zenith.world.items.component.EquipmentComponent.class);
        if (eq != null) {
            boolean alreadyInStrictSlot = false;
            for (SlotGroup group : groups) {
                if (!group.isActive()) continue;
                for (com.za.zenith.entities.inventory.Slot slot : group.getSlots()) {
                    if (slot == originSlot && !slot.getType().equals("any")) {
                        alreadyInStrictSlot = true;
                        break;
                    }
                }
            }
            
            if (!alreadyInStrictSlot) {
                for (SlotGroup group : groups) {
                    if (!group.isActive()) continue;
                    for (com.za.zenith.entities.inventory.Slot slot : group.getSlots()) {
                        if (slot.getType().equals(eq.getSlotType()) && slot.getStack() == null) {
                            slot.setStack(stack);
                            originSlot.setStack(null);
                            return;
                        }
                    }
                }
            }
        }

        // Определяем целевые группы для квик-мува. Если предмет в хотбаре - кидаем в карманы/рюкзак, иначе в хотбар.
        List<String> targetGroups = new ArrayList<>();
        boolean isFromHotbar = false;
        for (Slot slot : getGroup("hotbar").getSlots()) {
             if (slot == originSlot) isFromHotbar = true;
        }

        if (isFromHotbar) {
            targetGroups.add("pockets");
            targetGroups.add("pouch");
        } else {
            targetGroups.add("hotbar");
        }

        // 1. Try to stack in target groups
        for (String groupId : targetGroups) {
            SlotGroup group = getGroup(groupId);
            if (group == null || !group.isActive()) continue;
            
            for (com.za.zenith.entities.inventory.Slot slot : group.getSlots()) {
                ItemStack existing = slot.getStack();
                if (existing != null && existing.getItem().getId() == stack.getItem().getId() && !existing.isFull() && slot.isItemValid(stack)) {
                    int space = existing.getAvailableSpace();
                    int toMove = Math.min(space, stack.getCount());
                    
                    existing.setCount(existing.getCount() + toMove);
                    stack.setCount(stack.getCount() - toMove);
                    
                    if (stack.getCount() <= 0) {
                        originSlot.setStack(null);
                        return;
                    }
                }
            }
        }

        // 2. Try to find empty slot in target groups
        for (String groupId : targetGroups) {
            SlotGroup group = getGroup(groupId);
            if (group == null || !group.isActive()) continue;
            
            for (com.za.zenith.entities.inventory.Slot slot : group.getSlots()) {
                if (slot.getStack() == null && slot.isItemValid(stack)) {
                    slot.setStack(stack);
                    originSlot.setStack(null);
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
        ItemStack currentAcc = slots[SLOT_ACCESSORY];

        // Did the accessory change?
        if (lastAccessory != currentAcc) {
            SlotGroup pouch = getGroup("pouch");
            
            // Handle removal/change of the previous accessory
            if (lastAccessory != null && lastAccessory.getItem().hasComponent(BagComponent.class)) {
                BagComponent bag = lastAccessory.getItem().getComponent(BagComponent.class);
                com.za.zenith.world.inventory.ItemInventory itemInv = lastAccessory.getItemInventory();
                
                // If the bag specifies it should drop items when unequipped, drop them
                if (bag.isDropOnUnequip() && itemInv != null) {
                    for (int i = 0; i < itemInv.size(); i++) {
                        ItemStack innerStack = itemInv.getStack(i);
                        if (innerStack != null) {
                            dropStack(innerStack, player, world, camera, true);
                            itemInv.setStack(i, null);
                        }
                    }
                }
            }

            pouch.getSlots().clear();

            // Handle the new accessory
            if (currentAcc != null && currentAcc.getItem().hasComponent(BagComponent.class)) {
                com.za.zenith.world.inventory.ItemInventory itemInv = currentAcc.getItemInventory();
                if (itemInv != null) {
                    for (int i = 0; i < itemInv.size(); i++) {
                        // Create slots wrapping the ItemInventory instead of Player Inventory
                        pouch.addSlot(new Slot(itemInv, i, "any"));
                    }
                }
            }

            lastAccessory = currentAcc;
        }
    }

    public void dropSelected(Player player, World world, Camera camera, boolean fullStack) {
        ItemStack stack = getSelectedItemStack();
        if (stack != null) {
            dropStack(stack, player, world, camera, fullStack);
            if (stack.getCount() <= 0) setStack(selectedSlot, null);
        }
    }

    public List<ItemStack> getAllStacks() {
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < TOTAL_SIZE; i++) {
            if (slots[i] != null) all.add(slots[i]);
        }
        // Also check contents of bags/pouches
        for (SlotGroup group : groups) {
            if (group.getId().equals("pouch") && group.isActive()) {
                for (Slot slot : group.getSlots()) {
                    if (slot.getStack() != null) all.add(slot.getStack());
                }
            }
        }
        return all;
    }

    private void dropStack(ItemStack stack, Player player, World world, Camera camera, boolean fullStack) {
        Vector3f lookDir = new Vector3f(0, 0, -1).rotateX(camera.getRotation().x).rotateY(camera.getRotation().y).normalize();
        Vector3f pos = new Vector3f(player.getPosition()).add(0, 1.5f, 0).add(new Vector3f(lookDir).mul(0.5f));
        
        ItemStack toDrop = fullStack ? stack.copy() : stack.split(1);
        if (fullStack) stack.setCount(0);
        
        com.za.zenith.entities.ItemEntity entity = new com.za.zenith.entities.ItemEntity(pos, toDrop);
        entity.getVelocity().set(lookDir.x * 0.3f, lookDir.y * 0.3f + 0.1f, lookDir.z * 0.3f);
        world.spawnEntity(entity);
    }

    private void logSelection() {
        Item current = getSelectedItem();
        String name = (current != null) ? current.getName() : "EMPTY";
        com.za.zenith.utils.Logger.info("Selected slot %d: %s", selectedSlot, name);
    }
}


