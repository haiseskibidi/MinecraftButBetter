package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.world.TreecapitatorService;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.items.ItemStack;

public class FellingLogBlockDefinition extends BlockDefinition {
    public FellingLogBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean onBlockBreak(World world, BlockPos pos, Block block, Player player) {
        // Если игрок присел — ломаем сразу (как обычный блок)
        if (player.isSneaking()) {
            return true;
        }

        // Проверяем наличие топора
        ItemStack held = player.getInventory().getSelectedItemStack();
        if (held == null || !held.getItem().getIdentifier().getPath().contains("axe")) {
            return true; // Рукой ломаем по одному блоку
        }

        // Считаем высоту дерева над нами
        int logsAbove = TreecapitatorService.getInstance().countLogsAbove(world, pos);
        String currentId = getIdentifier().getPath(); // e.g. "felling_stage_1"
        
        boolean shouldFell = false;
        
        if (logsAbove == 0) {
            shouldFell = true; // Нет блоков сверху — падает мгновенно
        } else if (logsAbove <= 3) {
            // Маленькое дерево (до 3 блоков сверху) -> падает сразу после stage 1
            if (currentId.contains("stage_1")) shouldFell = true;
        } else if (logsAbove <= 6) {
            // Среднее дерево (4-6 блоков сверху) -> падает после stage 2
            if (currentId.contains("stage_2")) shouldFell = true;
        } else if (logsAbove <= 9) {
            // Крупное дерево (7-9 блоков сверху) -> падает после stage 3
            if (currentId.contains("stage_3")) shouldFell = true;
        }
        // Огромные деревья (10+) падают только после stage 4 (дефолтная логика)

        if (shouldFell) {
            TreecapitatorService.getInstance().fellTree(world, pos, player);
            return false;
        }

        // Определяем следующую стадию
        Identifier nextStageId = getNextStage();
        
        if (nextStageId != null) {
            BlockDefinition nextDef = BlockRegistry.getBlock(nextStageId);
            if (nextDef != null) {
                // Сохраняем метаданные (флаг натуральности и индекс дерева)
                world.setBlock(pos, new Block(nextDef.getId(), block.getMetadata()));
                com.za.minecraft.utils.Logger.info("Felling stage (Height: %d): %s -> %s", 
                    logsAbove, getIdentifier(), nextStageId);
                return false; // Блок заменен, не удаляем
            }
        }

        // Если следующей стадии нет — рубим всё дерево
        TreecapitatorService.getInstance().fellTree(world, pos, player);
        return false; // Блок уже удален внутри fellTree, возвращаем false чтобы не было двойного дропа
    }
}
