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
        Identifier nextStageId = getNextStage();
        
        if (nextStageId != null) {
            BlockDefinition nextDef = BlockRegistry.getBlock(nextStageId);
            if (nextDef != null) {
                // Сохраняем метаданные (индекс дерева)
                world.setBlock(pos, new Block(nextDef.getId(), block.getMetadata()));
                com.za.minecraft.utils.Logger.info("Felling stage: %s -> %s, woodIndex: %d", 
                    getIdentifier(), nextStageId, block.getMetadata());
                return false; // Блок заменен, не удаляем
            }
        }

        // Если следующей стадии нет — рубим всё дерево
        TreecapitatorService.getInstance().fellTree(world, pos, player);
        return true; // Блок удаляется
    }
}
