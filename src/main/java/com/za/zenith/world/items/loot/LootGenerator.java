package com.za.zenith.world.items.loot;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.ItemRegistry;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.stats.*;
import com.za.zenith.world.items.component.LootboxComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Service for generating dynamic items with rarities and affixes.
 */
public class LootGenerator {
    private static final Random RANDOM = new Random();

    public static List<ItemStack> generateFromCase(ItemStack caseStack) {
        List<ItemStack> result = new ArrayList<>();
        LootboxComponent comp = caseStack.getItem().getComponent(LootboxComponent.class);
        if (comp == null) return result;

        LootTable table = LootTableRegistry.get(comp.lootTable());
        if (table == null) return result;

        for (LootTable.Pool pool : table.pools()) {
            for (int i = 0; i < pool.rolls(); i++) {
                ItemStack item = rollItem(pool);
                if (item != null) {
                    applyRandomRarity(item, comp.rarityWeights());
                    applyRandomAffixes(item, comp.affixWeights());
                    result.add(item);
                }
            }
        }
        return result;
    }

    private static ItemStack rollItem(LootTable.Pool pool) {
        int totalWeight = pool.entries().stream().mapToInt(LootTable.Entry::weight).sum();
        if (totalWeight <= 0) return null;

        int roll = RANDOM.nextInt(totalWeight);
        int current = 0;
        for (LootTable.Entry entry : pool.entries()) {
            current += entry.weight();
            if (roll < current) {
                Item item = ItemRegistry.getItem(entry.item());
                return item != null ? new ItemStack(item) : null;
            }
        }
        return null;
    }

    private static void applyRandomRarity(ItemStack stack, Map<Identifier, Integer> rarityWeights) {
        if (rarityWeights != null && !rarityWeights.isEmpty()) {
            int totalWeight = rarityWeights.values().stream().mapToInt(Integer::intValue).sum();
            if (totalWeight > 0) {
                int roll = RANDOM.nextInt(totalWeight);
                int current = 0;
                for (Map.Entry<Identifier, Integer> entry : rarityWeights.entrySet()) {
                    current += entry.getValue();
                    if (roll < current) {
                        stack.setRarity(entry.getKey());
                        return;
                    }
                }
            }
        }

        // Fallback to global weights
        List<RarityDefinition> rarities = new ArrayList<>(RarityRegistry.getAll());
        int totalWeight = rarities.stream().mapToInt(RarityDefinition::weight).sum();
        
        int roll = RANDOM.nextInt(totalWeight);
        int current = 0;
        for (RarityDefinition def : rarities) {
            current += def.weight();
            if (roll < current) {
                stack.setRarity(def.identifier());
                break;
            }
        }
    }

    private static void applyRandomAffixes(ItemStack stack, Map<Identifier, Integer> affixWeights) {
        RarityDefinition itemRarity = RarityRegistry.get(stack.getRarity());
        if (itemRarity == null || itemRarity.affixSlots() <= 0) return;

        for (int i = 0; i < itemRarity.affixSlots(); i++) {
            Identifier tierId = rollAffixTier(affixWeights);
            if (tierId == null) continue;

            List<AffixDefinition> available = AffixRegistry.getByRarity(tierId);
            // Filter by item tags
            List<AffixDefinition> compatible = available.stream()
                .filter(a -> isCompatible(stack.getItem(), a))
                .toList();

            if (!compatible.isEmpty()) {
                AffixDefinition selected = compatible.get(RANDOM.nextInt(compatible.size()));
                stack.addAffix(selected.identifier());
            }
        }
    }

    private static Identifier rollAffixTier(Map<Identifier, Integer> weights) {
        if (weights == null || weights.isEmpty()) return null;
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWeight <= 0) return null;

        int roll = RANDOM.nextInt(totalWeight);
        int current = 0;
        for (Map.Entry<Identifier, Integer> entry : weights.entrySet()) {
            current += entry.getValue();
            if (roll < current) return entry.getKey();
        }
        return null;
    }

    private static boolean isCompatible(Item item, AffixDefinition affix) {
        if (affix.applicableTo().isEmpty()) return true;
        
        for (String tag : affix.applicableTo()) {
            if (tag.startsWith("tag:")) {
                String actualTag = tag.substring(4);
                if (item.hasTag(actualTag)) return true;
            } else {
                if (item.getIdentifier().toString().equals(tag)) return true;
            }
        }
        return false;
    }
}
