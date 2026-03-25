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

        // Определяем следующую стадию
        String currentPath = getIdentifier().getPath(); // например felling_stage_1
        int stageNum = Integer.parseInt(currentPath.substring(currentPath.length() - 1));
        
        if (stageNum < 4) {
            String nextStageId = "minecraft:felling_stage_" + (stageNum + 1);
            int nextBlockId = BlockRegistry.getBlock(Identifier.of(nextStageId)).getId();
            
            // Сохраняем метаданные (индекс дерева)
            world.setBlock(pos, new Block(nextBlockId, block.getMetadata()));
            com.za.minecraft.utils.Logger.info("Felling stage: " + stageNum + " -> " + (stageNum + 1) + ", woodIndex: " + block.getMetadata());
            return false; // Блок заменен, не удаляем
        } else {
            // Последняя стадия — рубим всё дерево
            TreecapitatorService.getInstance().fellTree(world, pos, player);
            return true; // Блок удаляется
        }
    }
}
