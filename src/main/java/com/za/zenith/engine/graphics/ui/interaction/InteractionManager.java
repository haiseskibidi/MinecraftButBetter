package com.za.zenith.engine.graphics.ui.interaction;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.entity.BlockEntity;
import com.za.zenith.world.items.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Менеджер правил взаимодействия. Загружает данные из JSON и подбирает лучшие подсказки.
 */
public class InteractionManager {
    private static final Map<Identifier, List<InteractionRule>> registry = new HashMap<>();
    private static final Gson gson = new Gson();

    public static void init() {
        try (InputStream in = InteractionManager.class.getResourceAsStream("/zenith/registry/interactions.json")) {
            if (in == null) {
                Logger.warn("interactions.json not found, HUD hints will be limited");
                return;
            }
            Type type = new TypeToken<Map<String, List<InteractionRule>>>(){}.getType();
            Map<String, List<InteractionRule>> data = gson.fromJson(new InputStreamReader(in), type);
            
            registry.clear();
            for (Map.Entry<String, List<InteractionRule>> entry : data.entrySet()) {
                Identifier id = Identifier.of(entry.getKey());
                registry.put(id, entry.getValue());
                Logger.info("REGISTERED INTERACTION: " + id + " (" + entry.getValue().size() + " rules)");
            }
            Logger.info("InteractionManager initialized with %d blocks", registry.size());
        } catch (Exception e) {
            Logger.error("Failed to load interactions.json: " + e.getMessage());
        }
    }

    public static InteractionRule getBestRule(com.za.zenith.entities.Player player, Identifier blockId, BlockEntity be, ItemStack held, com.za.zenith.utils.Direction side, org.joml.Vector3f localHit) {
        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(blockId);
        if (def != null && localHit != null) {
            com.za.zenith.world.World world = com.za.zenith.engine.core.GameLoop.getInstance().getWorld();
            // Если у блока есть зоны взаимодействия, проверяем попадание в них
            if (!def.getInteractionZones(world, be != null ? be.getPos() : null).isEmpty()) {
                if (!def.isInteractableAt(world, be != null ? be.getPos() : null, localHit)) {
                    return null;
                }
            }
        }

        List<InteractionRule> rules = registry.get(blockId);
        if (rules == null) return null;

        for (InteractionRule rule : rules) {
            if (checkRule(player, rule, be, held, side, localHit)) {
                return rule;
            }
        }
        
        return null;
    }

    private static boolean checkRule(com.za.zenith.entities.Player player, InteractionRule rule, BlockEntity be, ItemStack held, com.za.zenith.utils.Direction side, org.joml.Vector3f localHit) {
        // 1. Check minY
        if (rule.minY() != null) {
            if (localHit == null || localHit.y < rule.minY()) return false;
        }

        // 2. Check Held Item
        if (rule.heldItem() != null) {
            if (held == null || !held.getItem().getIdentifier().toString().equals(rule.heldItem())) return false;
        }
        
        // 2. Check Held Tag
        if (rule.heldTag() != null) {
            if (held == null || !held.getItem().hasTag(rule.heldTag())) return false;
        }

        // 2.1 Check Block Item Tag
        if (rule.blockItemTag() != null) {
            if (be instanceof BlockInfoProvider provider) {
                com.za.zenith.world.items.ItemStack beStack = provider.getHeldStack();
                if (beStack == null || !beStack.getItem().hasTag(rule.blockItemTag())) return false;
            } else return false;
        }

        // 2.2 Check Sneak
        if (rule.sneak() != null) {
            if (player.isSneaking() != rule.sneak()) return false;
        }

        // 2.3 Check Hand State
        if (rule.handEmpty() != null) {
            if ((held == null) != rule.handEmpty()) return false;
        }
        if (rule.handFull() != null) {
            if ((held != null) != rule.handFull()) return false;
        }

        // 3. Check Condition
        if (rule.condition() != null) {
            if (be instanceof BlockInfoProvider provider) {
                String status = provider.getDynamicStatus();
                switch (rule.condition()) {
                    case "has_item": return "has_item".equals(status);
                    case "is_empty": return "empty".equals(status);
                    case "is_burning": 
                        return be instanceof com.za.zenith.world.blocks.entity.PitKilnBlockEntity pk && pk.isBurning();
                }
            } else return false;
        }

        return true;
    }
}
