package com.za.zenith.world;

import com.za.zenith.entities.Player;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.blocks.WoodTypeRegistry;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.utils.Logger;

import java.util.*;

/**
 * Сервис для реализации механики Treecapitator.
 * Позволяет срубать все дерево целиком, если оно натуральное.
 */
public class TreecapitatorService {
    private static TreecapitatorService instance;
    private static final int MAX_BLOCKS = 256;

    public static TreecapitatorService getInstance() {
        if (instance == null) {
            instance = new TreecapitatorService();
        }
        return instance;
    }

    public void fellTree(World world, BlockPos startPos, Player player) {
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> toDestroy = new ArrayList<>();
        
        queue.add(startPos);
        visited.add(startPos);
        
        while (!queue.isEmpty() && toDestroy.size() < MAX_BLOCKS) {
            BlockPos current = queue.poll();
            toDestroy.add(current);
            
            // Проверяем соседей в радиусе 1 блока (включая диагонали)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        
                        BlockPos neighbor = new BlockPos(current.x() + dx, current.y() + dy, current.z() + dz);
                        
                        // Условие: не ниже начального блока и еще не посещали
                        if (neighbor.y() < startPos.y() || visited.contains(neighbor)) continue;
                        
                        Block block = world.getBlock(neighbor);
                        if (block.isAir()) continue;
                        
                        // Проверка на натуральность и тэг treecapitator
                        if (block.isNatural() && BlockRegistry.getBlock(block.getType()).hasTag("treecapitator")) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        
        Logger.info("Treecapitator: Felling tree with %d blocks starting from %s", toDestroy.size(), startPos);
        
        // Карта для накопления дропа
        Map<Integer, Integer> accumulatedDrop = new HashMap<>();
        
        for (BlockPos pos : toDestroy) {
            Block block = world.getBlock(pos);
            int type = block.getType();
            int metadata = block.getMetadata();
            
            // Resolve technical stages to actual logs
            com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(type);
            int finalType = type;
            if (def instanceof com.za.zenith.world.blocks.FellingLogBlockDefinition) {
                Identifier logId = WoodTypeRegistry.getLogId(metadata & 0x7F); // Ignore BIT_NATURAL
                if (logId != null) {
                    finalType = BlockRegistry.getRegistry().getId(logId);
                }
            }

            accumulatedDrop.put(finalType, accumulatedDrop.getOrDefault(finalType, 0) + 1);
            
            // Удаляем блок без вызова onDestroyed (чтобы избежать рекурсии)
            world.setBlock(pos, new Block(Blocks.AIR.getId()));
        }
        
        // Спавним накопленный дроп в одной точке
        for (Map.Entry<Integer, Integer> entry : accumulatedDrop.entrySet()) {
            com.za.zenith.world.items.Item item = com.za.zenith.world.items.ItemRegistry.getItem(entry.getKey());
            if (item != null) {
                int count = entry.getValue();
                // Спавним стаками по 64
                while (count > 0) {
                    int stackSize = Math.min(count, 64);
                    world.spawnItem(new ItemStack(item, stackSize), startPos.x() + 0.5f, startPos.y() + 0.5f, startPos.z() + 0.5f);
                    count -= stackSize;
                }
            }
        }
    }

    public int countLogsAbove(World world, BlockPos pos) {
        int count = 0;
        BlockPos current = pos.up();
        while (count < 32) {
            Block b = world.getBlock(current);
            if (b.isAir()) break;
            
            com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(b.getType());
            if (def != null && def.getIdentifier().getPath().contains("log")) {
                count++;
                current = current.up();
            } else {
                break;
            }
        }
        return count;
    }
}


