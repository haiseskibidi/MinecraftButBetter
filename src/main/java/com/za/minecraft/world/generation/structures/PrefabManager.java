package com.za.minecraft.world.generation.structures;

import com.za.minecraft.world.blocks.BlockType;

import java.util.HashMap;
import java.util.Map;

public class PrefabManager {
    public static final StructureTemplate RUINED_HOUSE_1;
    public static final StructureTemplate SMALL_STORE;
    public static final StructureTemplate APARTMENT_BUILDING;
    public static final StructureTemplate SKYSCRAPER_FLOOR;
    public static final StructureTemplate SKYSCRAPER_ROOF;

    static {
        // --- РУИНЫ ДОМА (С ИНТЕРЬЕРОМ) ---
        Map<Character, Byte> housePalette = new HashMap<>();
        housePalette.put('#', BlockType.BRICKS);
        housePalette.put('C', BlockType.GRAY_CONCRETE);
        housePalette.put('G', BlockType.GLASS);
        housePalette.put('M', BlockType.RUSTY_METAL);
        housePalette.put('F', BlockType.COBBLESTONE); // Пол
        housePalette.put('W', BlockType.WOOD); // Стол
        housePalette.put('B', BlockType.BOOKSHELF); // Шкаф
        housePalette.put('L', BlockType.LEAVES); // Заросший угол
        housePalette.put(' ', (byte)-1); // Игнорировать
        housePalette.put('.', BlockType.AIR); // Воздух внутри

        String[][] houseLayers = {
            { // Y = 0 (Пол и часть мебели)
                "#######",
                "#FFFFF#",
                "#F.B.F#",
                "#F.W.F#",
                "#FFFFF#",
                "### ###"
            },
            { // Y = 1 (Стены, окна, мебель)
                "##G#G##",
                "#L....#",
                "G..B..#",
                "#..W..#",
                "#L....#",
                "### ###" 
            },
            { // Y = 2 (Верх стен и потолок)
                "#######",
                "#.....#",
                "#.....#",
                "##M####", // Ржавая балка
                "#.....#",
                "#######"
            },
            { // Y = 3 (Разрушенный верх)
                "##   ##",
                "#     #",
                "       ",
                "#     #",
                "#     #",
                " ###   "
            }
        };
        RUINED_HOUSE_1 = StructureTemplate.parse(houseLayers, housePalette);

        // --- МАГАЗИН (С ПРИЛАВКОМ И ЗАРОСЛЯМИ) ---
        Map<Character, Byte> storePalette = new HashMap<>();
        storePalette.put('C', BlockType.CYAN_CONCRETE);
        storePalette.put('W', BlockType.WHITE_CONCRETE);
        storePalette.put('G', BlockType.GLASS);
        storePalette.put('S', BlockType.STONE_BRICKS);
        storePalette.put('O', BlockType.OAK_PLANKS); // Прилавок
        storePalette.put('L', BlockType.LEAVES); // Растения
        storePalette.put(' ', (byte)-1);
        storePalette.put('.', BlockType.AIR);

        String[][] storeLayers = {
            { // Y = 0
                "CCCCCCC",
                "CSSSSSC",
                "CSOOOSC", // Прилавок
                "CSL..SC",
                "CCGGGCC"
            },
            { // Y = 1
                "CCCCCCC",
                "W.....W",
                "W.....W",
                "WL....W",
                "CWGGGCW"
            },
            { // Y = 2
                "CCCCCCC",
                "CW...WC",
                "CW...WC",
                "CW...WC",
                "CCCCCCC"
            },
            { // Y = 3 (Крыша)
                "CCCCCCC",
                "CCCCCCC",
                "C   CCC", // Дыра в крыше
                "CCCCCCC",
                "CCCCCCC"
            }
        };
        SMALL_STORE = StructureTemplate.parse(storeLayers, storePalette);

        // --- ЖИЛОЙ ДОМ (С ПЕРЕГОРОДКАМИ) ---
        Map<Character, Byte> aptPalette = new HashMap<>();
        apartmentPaletteSetup(aptPalette);
        String[][] aptLayers = {
            { // Y = 0 (Пол и стены)
                "WWWWWWWWW",
                "WFFFFFFFW",
                "WFFFWFFFW", // Внутренняя стена
                "WFFFWFFFW",
                "WFFFWFFFW",
                "WWWW WWWW"
            },
            { // Y = 1 (Интерьер)
                "WGGGWGGGW",
                "W.B.W.L.W",
                "G...W...G",
                "W...W...W",
                "W.O...O.W",
                "WWWW WWWW"
            },
            { // Y = 2
                "WWWWWWWWW",
                "W...W...W",
                "G...W...G",
                "W...W...W",
                "W.......W",
                "WWWWWWWWW"
            },
            { // Y = 3 (Потолок/Пол след. этажа)
                "WWWWWWWWW",
                "WFFFFFFFW",
                "WFFFFFFFW",
                "WFFFFFFFW",
                "WFFFFFFFW",
                "WWWWWWWWW"
            }
        };
        APARTMENT_BUILDING = StructureTemplate.parse(aptLayers, aptPalette);

        // --- БЛОК НЕБОСКРЕБА (1 этаж) ---
        Map<Character, Byte> skyPalette = new HashMap<>();
        skyPalette.put('M', BlockType.RUSTY_METAL);
        skyPalette.put('G', BlockType.GLASS);
        skyPalette.put('F', BlockType.COBBLESTONE);
        skyPalette.put('B', BlockType.BOOKSHELF); // Офисные шкафы
        skyPalette.put(' ', (byte)-1);
        skyPalette.put('.', BlockType.AIR);

        String[][] skyFloorLayers = {
            { // Y = 0
                "MMMMMMM",
                "MFFFFFM",
                "MFFFFFM",
                "MFFFFFM",
                "MMMMMMM"
            },
            { // Y = 1 (Окна и шкафы у стены)
                "MGGGGGM",
                "G..B..G",
                "G.....G",
                "G..B..G",
                "MGGGGGM"
            },
            { // Y = 2
                "MGGGGGM",
                "G.....G",
                "G.....G",
                "G.....G",
                "MGGGGGM"
            },
            { // Y = 3
                "MMMMMMM",
                "M.....M",
                "M.....M",
                "M.....M",
                "MMMMMMM"
            }
        };
        SKYSCRAPER_FLOOR = StructureTemplate.parse(skyFloorLayers, skyPalette);

        // --- РАЗРУШЕННАЯ КРЫША НЕБОСКРЕБА ---
        String[][] skyRoofLayers = {
            { // Y = 0
                "MMMMMMM",
                "MFFFFFM",
                "MF F FM",
                "MFFFFFM",
                "MMMMMMM"
            },
            { // Y = 1
                "M   G M",
                "       ",
                "       ",
                "  G    ",
                "M     M"
            }
        };
        SKYSCRAPER_ROOF = StructureTemplate.parse(skyRoofLayers, skyPalette);
    }
    
    private static void apartmentPaletteSetup(Map<Character, Byte> p) {
        p.put('W', BlockType.WHITE_CONCRETE);
        p.put('F', BlockType.OAK_PLANKS); // Паркет
        p.put('G', BlockType.GLASS);
        p.put('B', BlockType.BOOKSHELF);
        p.put('O', BlockType.WOOD); // Стол
        p.put('L', BlockType.LEAVES); // Растение в горшке
        p.put(' ', (byte)-1);
        p.put('.', BlockType.AIR);
    }
}
