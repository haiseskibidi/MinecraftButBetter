package com.za.minecraft.world.blocks;

public class BlockExample {

    public static void init() {
        String base = "minecraft/textures/block/";

        // 1. Создание блока с одинаковой текстурой со всех сторон
        BlockRegistry.registerBlock(new BlockDefinition(BlockType.GOLD_ORE, "gold_ore", true, false)
                .setTextures(new BlockTextures(base + "gold_ore.png")));

        // 2. Создание блока с разными текстурами для сторон
        // (Требуется добавить новый ID в BlockType.java)
        // BlockRegistry.registerBlock(new BlockDefinition((byte)17, "custom_block", true, false).setTextures(new BlockTextures(
        //     base + "custom_block_top.png",
        //     base + "custom_block_bottom.png",
        //     base + "custom_block_side.png"
        // )));

        // 3. Перезапись текстур для существующих блоков
        // Например, если хочешь изменить текстуру угольной руды:
        BlockRegistry.registerBlock(new BlockDefinition(BlockType.COAL_ORE, "coal_ore", true, false).setTextures(new BlockTextures(
            base + "my_custom_coal_ore.png"
        )));
    }
}
