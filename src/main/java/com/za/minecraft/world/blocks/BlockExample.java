package com.za.minecraft.world.blocks;

/**
 * Пример того, как легко добавить новые блоки в систему
 * 
 * 1. Добавь новый тип в BlockType enum
 * 2. Зарегистрируй текстуры в BlockRegistry
 * 3. Добавь в инвентарь (опционально)
 */
public class BlockExample {
    
    public static void addNewBlocks() {
        String base = "minecraft/textures/block/";
        
        // Пример 1: Простой блок (все грани одинаковые)
        // BlockRegistry.registerSimpleBlock(BlockType.DIAMOND_BLOCK, base + "diamond_block.png");
        
        // Пример 2: Блок с разными верхом/низом/боками (как дерево)
        // BlockRegistry.registerTopBottomSidesBlock(
        //     BlockType.BIRCH_LOG,
        //     base + "birch_log_top.png",    // верх
        //     base + "birch_log_top.png",    // низ
        //     base + "birch_log.png"         // бока
        // );
        
        // Пример 3: Полный контроль над каждой гранью
        // BlockRegistry.registerBlock(BlockType.CUSTOM_BLOCK, new BlockTextures(
        //     base + "top_texture.png",     // верх
        //     base + "bottom_texture.png",  // низ
        //     base + "north_texture.png",   // север (-Z)
        //     base + "south_texture.png",   // юг (+Z)
        //     base + "east_texture.png",    // восток (+X)
        //     base + "west_texture.png"     // запад (-X)
        // ));
        
        // Пример добавления печки с анимированной передней гранью
        BlockRegistry.registerBlock(BlockType.COAL_ORE, new BlockTextures(
            base + "furnace_top.png",      // верх
            base + "furnace_top.png",      // низ
            base + "furnace_side.png",     // север
            base + "furnace_front.png",    // юг (передняя грань)
            base + "furnace_side.png",     // восток
            base + "furnace_side.png"      // запад
        ));
    }
    
    /**
     * Шаги для добавления нового блока:
     * 
     * 1. В BlockType.java добавь:
     *    NEW_BLOCK(17, "new_block", true, false),
     * 
     * 2. В BlockRegistry.java в static блоке добавь:
     *    BLOCK_TEXTURES.put(BlockType.NEW_BLOCK, new BlockTextures(base + "new_texture.png"));
     * 
     * 3. В Inventory.java в массив AVAILABLE_BLOCKS добавь:
     *    BlockType.NEW_BLOCK
     * 
     * 4. Положи текстуру в: src/main/resources/minecraft/textures/block/new_texture.png
     * 
     * Готово! Блок автоматически:
     * - Загрузится в атлас текстур
     * - Будет правильно рендериться
     * - Появится в инвентаре для постройки
     * - Будет поддерживать ломание/размещение
     */
}
