package com.za.zenith.world.generation.structures;

import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Blocks;

import java.util.Map;

public class StructureTemplate {
    private final int width;
    private final int height;
    private final int depth;
    private final int[][][] blocks; // [y][z][x]

    public StructureTemplate(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = new int[height][depth][width];
    }

    public void setBlock(int x, int y, int z, int type) {
        if (x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth) {
            blocks[y][z][x] = type;
        }
    }

    public void build(World world, int startX, int startY, int startZ) {
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    int blockId = blocks[y][z][x];
                    // -1 означает "не заменять блок" (игнорировать)
                    if (blockId != -1) {
                        world.setBlock(startX + x, startY + y, startZ + z, blockId);
                    }
                }
            }
        }
    }

    /**
     * Парсит структуру из слоев (ASCII-арт).
     * @param layers Массив 2D слоев снизу вверх. Каждый слой - массив строк (одна строка = ось X, массив = ось Z).
     * @param palette Маппинг символов в ID блоков.
     */
    public static StructureTemplate parse(String[][] layers, Map<Character, Integer> palette) {
        int h = layers.length;
        int d = layers[0].length;
        int w = layers[0][0].length();

        StructureTemplate template = new StructureTemplate(w, h, d);

        for (int y = 0; y < h; y++) {
            for (int z = 0; z < d; z++) {
                String row = layers[y][z];
                for (int x = 0; x < w; x++) {
                    char c = row.charAt(x);
                    int blockId = palette.getOrDefault(c, -1); // -1 по умолчанию
                    template.setBlock(x, y, z, blockId);
                }
            }
        }
        return template;
    }
    
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDepth() { return depth; }
}


