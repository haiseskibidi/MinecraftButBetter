package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.world.TreecapitatorService;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.Items;
import com.za.minecraft.world.blocks.entity.StumpBlockEntity;

public class LogBlockDefinition extends BlockDefinition {
    public LogBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (heldStack == null) return false;

        Identifier heldId = heldStack.getItem().getIdentifier();
        
        // Ищем рецепт обтёсывания для этого блока
        java.util.List<com.za.minecraft.world.recipes.IRecipe> recipes = com.za.minecraft.world.recipes.RecipeRegistry.getRecipesByType("carving");
        
        if (recipes.isEmpty()) {
            com.za.minecraft.utils.Logger.info("No carving recipes found in registry");
        }

        for (com.za.minecraft.world.recipes.IRecipe r : recipes) {
            com.za.minecraft.world.recipes.CarvingRecipe recipe = (com.za.minecraft.world.recipes.CarvingRecipe) r;
            
            boolean blockMatch = recipe.getInputBlock().equals(this.getIdentifier());
            boolean toolMatch = (recipe.getTool() == null || heldId.equals(recipe.getTool()));
            boolean sneaking = player.isSneaking();
            
            if (blockMatch) {
                com.za.minecraft.utils.Logger.info("Carving debug: toolMatch=%b (held:%s, req:%s), sneaking=%b, hitY=%.2f", 
                    toolMatch, heldId, recipe.getTool(), sneaking, hitY);
            }

            // Пень можно сделать только если бревно стоит вертикально (DIR_UP / Y-axis)
            Block currentBlock = world.getBlock(pos);
            if (currentBlock.getDirection() != Block.DIR_UP) {
                continue; 
            }

            if (blockMatch && toolMatch && sneaking && hitY > 0.8f) {
                int intermediateId = BlockRegistry.getRegistry().getId(recipe.getIntermediateBlock());
                world.setBlock(pos, new Block(intermediateId));
                
                StumpBlockEntity be = (StumpBlockEntity) world.getBlockEntity(pos);
                if (be != null) {
                    be.setCarvingMask(0);
                    // Применяем первый удар сразу, чтобы не нужно было нажимать 2 раза
                    int index = CarvingLayoutEngine.getZoneIndex(hitX, hitZ);
                    be.setCarvingBit(index);
                }
                
                com.za.minecraft.utils.Logger.info("Started carving %s into %s at zone %d", recipe.getInputBlock(), recipe.getIntermediateBlock(), CarvingLayoutEngine.getZoneIndex(hitX, hitZ));
                return true;
            }
        }

        // 1. Старая логика топора (оставляем как быстрый вариант, если нет рецепта)
        if (heldId.getPath().contains("axe")) {
            Block currentBlock = world.getBlock(pos);
            if (currentBlock.getDirection() == Block.DIR_UP) {
                world.setBlock(pos, new Block(Blocks.STUMP.getId()));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onBlockBreak(World world, BlockPos pos, Block block, Player player) {
        // Если игрок присел или это не натуральное дерево — ломаем сразу
        if (player.isSneaking() || !block.isNatural()) {
            return true;
        }

        // Проверяем наличие топора
        ItemStack held = player.getInventory().getSelectedItemStack();
        if (held == null || !held.getItem().getIdentifier().getPath().contains("axe")) {
            return true; // Рукой ломаем по одному блоку
        }

        // Проверяем, есть ли что-то над нами. Если нет — рубим как обычный блок
        int logsAbove = com.za.minecraft.world.TreecapitatorService.getInstance().countLogsAbove(world, pos);
        if (logsAbove == 0) {
            return true;
        }

        // Всегда начинаем с первой стадии (minecraft:felling_stage_1)
        Identifier stage1Id = Identifier.of("minecraft:felling_stage_1");
        
        int woodIndex = WoodTypeRegistry.getIndex(this.getIdentifier());
        if (woodIndex < 0) woodIndex = 0; 
        
        BlockDefinition stageDef = BlockRegistry.getBlock(stage1Id);
        if (stageDef != null) {
            // Preserve BIT_NATURAL flag
            byte metadata = (byte)((block.getMetadata() & Block.BIT_NATURAL) | (woodIndex & 0x7F));
            world.setBlock(pos, new Block(stageDef.getId(), metadata));
            com.za.minecraft.utils.Logger.info("Log started felling: converted to %s, woodIndex: %d", stage1Id, woodIndex);
            return false;
        }

        return true;
    }

    @Override
    public void onDestroyed(World world, BlockPos pos, Block block, Player player) {
        // Логика перенесена в onBlockBreak для постепенного срубания
    }
}
