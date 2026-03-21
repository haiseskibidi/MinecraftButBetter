package com.za.minecraft.world;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.utils.Logger;
import com.za.minecraft.world.blocks.BlockDefinition;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.blocks.BlockTextures;
import com.za.minecraft.world.blocks.BlockTypeRegistry;
import com.za.minecraft.world.items.FoodItem;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemTypeRegistry;
import com.za.minecraft.world.items.ToolItem;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.component.FoodComponent;
import com.za.minecraft.world.items.component.FuelComponent;
import com.za.minecraft.world.items.component.ToolComponent;
import com.za.minecraft.world.recipes.NappingRecipe;
import com.za.minecraft.world.recipes.RecipeRegistry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {
    private static final Gson GSON = new Gson();

    public static void loadAll() {
        // Гарантируем наличие AIR даже если загрузка из JSON не сработает
        BlockRegistry.registerBlock(new BlockDefinition(0, "block.minecraft.air", false, false));
        
        List<String> namespaces = loadNamespaces();
        for (String ns : namespaces) {
            loadBlocks(ns);
        }
        com.za.minecraft.utils.events.RegistryEvents.fireBlockRegistration();
        
        for (String ns : namespaces) {
            loadItems(ns);
        }
        com.za.minecraft.utils.events.RegistryEvents.fireItemRegistration();

        for (String ns : namespaces) {
            loadStructures(ns);
        }

        for (String ns : namespaces) {
            loadRecipes(ns);
        }
    }

    private static void loadRecipes(String namespace) {
        List<String> files = listResources(namespace + "/recipes");
        if (!files.isEmpty()) {
            for (String file : files) {
                loadResource(namespace + "/recipes/" + file, DataLoader::parseRecipe);
            }
        }
    }

    private static void parseRecipe(JsonElement el) {
        try {
            JsonObject obj = el.getAsJsonObject();
            Identifier id = Identifier.of(obj.get("identifier").getAsString());
            String type = obj.get("type").getAsString();

            if (type.equalsIgnoreCase("napping")) {
                Identifier input = Identifier.of(obj.get("input").getAsString());
                JsonObject resObj = obj.getAsJsonObject("result");
                com.za.minecraft.world.items.Item resItem = com.za.minecraft.world.items.ItemRegistry.getItem(Identifier.of(resObj.get("item").getAsString()));
                int count = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
                ItemStack result = new ItemStack(resItem, count);

                JsonArray patternArr = obj.getAsJsonArray("pattern");
                boolean[] pattern = new boolean[25];
                for (int i = 0; i < 5; i++) {
                    String line = patternArr.get(i).getAsString();
                    for (int j = 0; j < 5; j++) {
                        pattern[i * 5 + j] = line.charAt(j) == '#';
                    }
                }
                RecipeRegistry.register(new NappingRecipe(id, input, result, pattern));
            }
        } catch (Exception e) {
            Logger.error("Failed to parse recipe: " + e.getMessage());
        }
    }

    private static List<String> loadNamespaces() {
        List<String> namespaces = new ArrayList<>();
        // По умолчанию всегда есть minecraft
        namespaces.add("minecraft");
        
        try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream("namespaces.index")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    String ns = line.trim();
                    if (!ns.isEmpty() && !namespaces.contains(ns)) {
                        namespaces.add(ns);
                    }
                }
            }
        } catch (Exception e) {
            Logger.warn("Could not read namespaces.index, using default 'minecraft'");
        }
        return namespaces;
    }

    private static void loadBlocks(String namespace) {
        List<String> files = listResources(namespace + "/blocks");
        if (files.isEmpty()) {
            Logger.warn("No blocks found in namespace: " + namespace);
        } else {
            for (String file : files) {
                loadResource(namespace + "/blocks/" + file, DataLoader::parseBlock);
            }
        }
    }

    private static void loadItems(String namespace) {
        List<String> files = listResources(namespace + "/items");
        if (!files.isEmpty()) {
            for (String file : files) {
                loadResource(namespace + "/items/" + file, DataLoader::parseItem);
            }
        }
    }

    private static void loadStructures(String namespace) {
        List<String> files = listResources(namespace + "/structures");
        if (!files.isEmpty()) {
            for (String file : files) {
                loadResource(namespace + "/structures/" + file, DataLoader::parseStructure);
            }
        }
    }

    private static void parseStructure(JsonElement el) {
        try {
            JsonObject obj = el.getAsJsonObject();
            Identifier id = Identifier.of(obj.get("identifier").getAsString());
            
            // Парсинг палитры
            JsonObject paletteObj = obj.getAsJsonObject("palette");
            java.util.Map<Character, Integer> palette = new java.util.HashMap<>();
            for (String key : paletteObj.keySet()) {
                char symbol = key.charAt(0);
                String blockIdStr = paletteObj.get(key).getAsString();
                // Если число — это старый формат или -1
                if (blockIdStr.matches("-?\\d+")) {
                    palette.put(symbol, Integer.parseInt(blockIdStr));
                } else {
                    palette.put(symbol, BlockRegistry.getRegistry().getId(Identifier.of(blockIdStr)));
                }
            }

            // Парсинг слоев
            JsonArray layersArr = obj.getAsJsonArray("layers");
            String[][] layers = new String[layersArr.size()][];
            for (int y = 0; y < layersArr.size(); y++) {
                JsonArray rowArr = layersArr.get(y).getAsJsonArray();
                layers[y] = new String[rowArr.size()];
                for (int z = 0; z < rowArr.size(); z++) {
                    layers[y][z] = rowArr.get(z).getAsString();
                }
            }

            com.za.minecraft.world.generation.structures.StructureTemplate template = 
                com.za.minecraft.world.generation.structures.StructureTemplate.parse(layers, palette);
            com.za.minecraft.world.generation.structures.StructureRegistry.register(id, template);
        } catch (Exception e) {
            Logger.error("Failed to parse structure: " + e.getMessage());
        }
    }

    private static void loadResource(String path, java.util.function.Consumer<JsonElement> parser) {
        try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) return;
            JsonElement root = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonElement.class);
            if (root.isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray()) {
                    parser.accept(el);
                }
            } else {
                parser.accept(root);
            }
        } catch (Exception e) {
            Logger.error("Failed to load resource " + path + ": " + e.getMessage());
        }
    }

    private static void parseBlock(JsonElement el) {
        try {
            JsonObject obj = el.getAsJsonObject();
            int id = obj.get("id").getAsInt();
            Identifier identifier = Identifier.of(obj.get("identifier").getAsString());
            String translationKey = obj.get("translationKey").getAsString();
            boolean solid = obj.get("solid").getAsBoolean();
            boolean transparent = obj.get("transparent").getAsBoolean();
            String type = obj.has("type") ? obj.get("type").getAsString() : "default";

            BlockDefinition def = BlockTypeRegistry.create(type, id, identifier, translationKey, solid, transparent);
            if (obj.has("hardness")) def.setHardness(obj.get("hardness").getAsFloat());
            if (obj.has("requiredTool")) def.setRequiredTool(obj.get("requiredTool").getAsString());
            if (obj.has("placement")) {
                def.setPlacementType(com.za.minecraft.world.blocks.PlacementType.valueOf(obj.get("placement").getAsString().toUpperCase()));
            }
            if (obj.has("textures")) {
                JsonObject tex = obj.getAsJsonObject("textures");
                String base = "minecraft/textures/block/";
                if (tex.has("all")) {
                    def.setTextures(new BlockTextures(base + tex.get("all").getAsString()));
                } else {
                    def.setTextures(new BlockTextures(
                        base + tex.get("top").getAsString(),
                        base + tex.get("bottom").getAsString(),
                        base + (tex.has("north") ? tex.get("north").getAsString() : tex.get("side").getAsString()),
                        base + (tex.has("south") ? tex.get("south").getAsString() : tex.get("side").getAsString()),
                        base + (tex.has("east") ? tex.get("east").getAsString() : tex.get("side").getAsString()),
                        base + (tex.has("west") ? tex.get("west").getAsString() : tex.get("side").getAsString())
                    ));
                }
            }
            BlockRegistry.registerBlock(def);
        } catch (Exception e) {
            Logger.error("Failed to parse block: " + e.getMessage());
        }
    }

    private static void parseItem(JsonElement el) {
        try {
            JsonObject obj = el.getAsJsonObject();
            int id = obj.get("id").getAsInt();
            Identifier identifier = Identifier.of(obj.get("identifier").getAsString());
            String translationKey = obj.get("translationKey").getAsString();
            String texture = obj.get("texture").getAsString();
            String type = obj.has("type") ? obj.get("type").getAsString() : "default";

            Item item = ItemTypeRegistry.create(type, (byte)id, identifier, translationKey, texture);
            
            if (obj.has("weight")) item.setWeight(obj.get("weight").getAsFloat());
            
            // Парсинг компонентов (перезаписывают дефолтные из конструктора)
            if (obj.has("components")) {
                JsonObject comps = obj.getAsJsonObject("components");
                
                if (comps.has("minecraft:food")) {
                    JsonObject f = comps.getAsJsonObject("minecraft:food");
                    item.addComponent(FoodComponent.class, new FoodComponent(
                        f.get("nutrition").getAsFloat(),
                        f.get("saturation").getAsFloat()
                    ));
                }
                
                if (comps.has("minecraft:tool")) {
                    JsonObject t = comps.getAsJsonObject("minecraft:tool");
                    item.addComponent(ToolComponent.class, new ToolComponent(
                        ToolItem.ToolType.valueOf(t.get("type").getAsString().toUpperCase()),
                        t.get("efficiency").getAsFloat(),
                        t.get("durability").getAsInt(),
                        t.has("isEffectiveAgainstAll") && t.get("isEffectiveAgainstAll").getAsBoolean()
                    ));
                }

                if (comps.has("minecraft:fuel")) {
                    JsonObject f = comps.getAsJsonObject("minecraft:fuel");
                    item.addComponent(FuelComponent.class, new FuelComponent(
                        f.get("fuelAmount").getAsFloat()
                    ));
                }
            }
            
            // Legacy поддержка или заполнение из корневых полей, если секции components нет
            if (type.equals("food") && (obj.has("nutrition") || obj.has("saturation"))) {
                item.addComponent(FoodComponent.class, new FoodComponent(
                    obj.has("nutrition") ? obj.get("nutrition").getAsFloat() : 0,
                    obj.has("saturation") ? obj.get("saturation").getAsFloat() : 0
                ));
            }
            if (type.equals("tool") && (obj.has("toolType") || obj.has("efficiency") || obj.has("durability"))) {
                item.addComponent(ToolComponent.class, new ToolComponent(
                    obj.has("toolType") ? ToolItem.ToolType.valueOf(obj.get("toolType").getAsString().toUpperCase()) : ToolItem.ToolType.NONE,
                    obj.has("efficiency") ? obj.get("efficiency").getAsFloat() : 1.0f,
                    obj.has("durability") ? obj.get("durability").getAsInt() : 0,
                    obj.has("isEffectiveAgainstAll") && obj.get("isEffectiveAgainstAll").getAsBoolean()
                ));
            }

            ItemRegistry.registerItem(item);
        } catch (Exception e) {
            Logger.error("Failed to parse item: " + e.getMessage());
        }
    }

    /**
     * Возвращает список файлов в папке ресурсов.
     * Работает через чтение списка из специального индексного файла или 
     * (в будущем) через сканирование JAR.
     */
    private static List<String> listResources(String path) {
        List<String> files = new ArrayList<>();
        // Для простоты реализации "сканирования" без сложных библиотек,
        // мы ищем файл '.index' в каждой папке ресурсов.
        try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(path + "/.index")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) files.add(line.trim());
                }
            }
        } catch (Exception e) {
            Logger.warn("Could not read index for " + path);
        }
        return files;
    }
}
