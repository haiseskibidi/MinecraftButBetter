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
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemTypeRegistry;
import com.za.minecraft.world.items.ToolType;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.component.FoodComponent;
import com.za.minecraft.world.items.component.FuelComponent;
import com.za.minecraft.world.items.component.ToolComponent;
import com.za.minecraft.engine.graphics.ui.GUIConfig;
import com.za.minecraft.engine.graphics.ui.GUIRegistry;
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
        BlockDefinition airDef = new BlockDefinition(0, "block.minecraft.air", false, false);
        airDef.setReplaceable(true);
        BlockRegistry.registerBlock(airDef);
        
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
            loadEntityDefinitions(ns);
        }

        for (String ns : namespaces) {
            loadStructures(ns);
        }

        for (String ns : namespaces) {
            loadRecipes(ns);
        }

        for (String ns : namespaces) {
            loadGUIs(ns);
        }

        loadScavengeSettings();
    }

    private static void loadGUIs(String namespace) {
        String path = namespace + "/gui";
        List<String> files = listResources(path);
        if (!files.isEmpty()) {
            for (String file : files) {
                loadResource(path + "/" + file, DataLoader::parseGUI);
            }
            Logger.info("Loaded GUIs for namespace: " + namespace);
        }
    }

    private static void parseGUI(JsonElement el) {
        try {
            JsonObject obj = el.getAsJsonObject();
            GUIConfig config = GSON.fromJson(obj, GUIConfig.class);
            com.za.minecraft.engine.graphics.ui.GUIRegistry.register(Identifier.of(config.id), config);
        } catch (Exception e) {
            Logger.error("Failed to parse GUI: " + e.getMessage());
        }
    }

    private static void loadScavengeSettings() {
        try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream("minecraft/registry/scavenge.json")) {
            if (is == null) return;
            JsonArray root = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
            for (JsonElement el : root) {
                JsonObject obj = el.getAsJsonObject();
                Identifier blockId = Identifier.of(obj.get("block").getAsString());
                float chance = obj.get("chance").getAsFloat();
                com.za.minecraft.world.generation.ScavengeSettings.register(blockId, chance);
            }
            Logger.info("Loaded scavenge settings");
        } catch (Exception e) {
            Logger.error("Failed to load scavenge settings: " + e.getMessage());
        }
    }

    private static void loadRecipes(String namespace) {
        String path = namespace + "/recipes";
        List<String> files = listResources(path);
        if (!files.isEmpty()) {
            for (String file : files) {
                Logger.info("Loading recipe file: " + file);
                loadResource(path + "/" + file, DataLoader::parseRecipe);
            }
            Logger.info("Loaded recipes for namespace: " + namespace);
        } else {
            Logger.warn("No recipes found in namespace: " + namespace + " (listResources returned empty)");
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
                Identifier resId = Identifier.of(resObj.get("item").getAsString());
                com.za.minecraft.world.items.Item resItem = com.za.minecraft.world.items.ItemRegistry.getItem(resId);
                
                if (resItem == null) {
                    Logger.error("Failed to parse recipe " + id + ": Result item " + resId + " not found!");
                    return;
                }

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
            } else if (type.equalsIgnoreCase("stump_crafting")) {
                Identifier input = Identifier.of(obj.get("input").getAsString());
                Identifier tool = obj.has("tool") ? Identifier.of(obj.get("tool").getAsString()) : null;
                int hits = obj.get("hits").getAsInt();
                
                JsonObject resObj = obj.getAsJsonObject("result");
                Identifier resId = Identifier.of(resObj.get("item").getAsString());
                int count = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
                
                com.za.minecraft.world.items.Item resItem = com.za.minecraft.world.items.ItemRegistry.getItem(resId);
                if (resItem != null) {
                    RecipeRegistry.register(new com.za.minecraft.world.recipes.StumpRecipe(id, input, tool, hits, new ItemStack(resItem, count)));
                } else {
                    Logger.error("Failed to parse stump recipe " + id + ": Result item " + resId + " not found!");
                }
            } else if (type.equalsIgnoreCase("in_world_crafting")) {
                JsonArray ingredientsArray = obj.getAsJsonArray("ingredients");
                List<Identifier> ingredients = new ArrayList<>();
                for (JsonElement e : ingredientsArray) {
                    ingredients.add(Identifier.of(e.getAsString()));
                }

                Identifier tool = obj.has("tool") ? Identifier.of(obj.get("tool").getAsString()) : null;
                int hits = obj.get("hits").getAsInt();

                JsonObject resObj = obj.getAsJsonObject("result");
                Identifier resId = Identifier.of(resObj.get("item").getAsString());
                int count = resObj.has("count") ? resObj.get("count").getAsInt() : 1;

                com.za.minecraft.world.items.Item resItem = com.za.minecraft.world.items.ItemRegistry.getItem(resId);
                if (resItem != null) {
                    RecipeRegistry.register(new com.za.minecraft.world.recipes.InWorldRecipe(id, ingredients, tool, hits, new ItemStack(resItem, count)));
                } else {
                    Logger.error("Failed to parse in_world recipe " + id + ": Result item " + resId + " not found!");
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to parse recipe: " + e.getMessage());
        }
    }

    private static void loadEntityDefinitions(String namespace) {
        List<String> files = listResources(namespace + "/entities");
        if (!files.isEmpty()) {
            for (String file : files) {
                loadResource(namespace + "/entities/" + file, DataLoader::parseEntityDefinition);
            }
            Logger.info("Loaded entity definitions for namespace: " + namespace);
        }
    }

    private static void parseEntityDefinition(JsonElement el) {
        try {
            JsonObject obj = el.getAsJsonObject();
            Identifier id = Identifier.of(obj.get("identifier").getAsString());
            String modelType = obj.get("modelType").getAsString();
            String texture = obj.get("texture").getAsString();
            
            org.joml.Vector3f visualScale = new org.joml.Vector3f(1.0f, 1.0f, 1.0f);
            if (obj.has("visualScale")) {
                if (obj.get("visualScale").isJsonArray()) {
                    JsonArray vs = obj.getAsJsonArray("visualScale");
                    visualScale.set(vs.get(0).getAsFloat(), vs.get(1).getAsFloat(), vs.get(2).getAsFloat());
                } else {
                    float s = obj.get("visualScale").getAsFloat();
                    visualScale.set(s, s, s);
                }
            }
            
            org.joml.Vector3f hitbox = new org.joml.Vector3f(0.5f, 0.5f, 0.5f);
            if (obj.has("hitbox")) {
                JsonArray h = obj.getAsJsonArray("hitbox");
                hitbox.set(h.get(0).getAsFloat(), h.get(1).getAsFloat(), h.get(2).getAsFloat());
            }

            com.za.minecraft.entities.EntityRegistry.register(
                new com.za.minecraft.entities.EntityDefinition(id, modelType, texture, visualScale, hitbox)
            );
        } catch (Exception e) {
            Logger.error("Failed to parse entity definition: " + e.getMessage());
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
            if (obj.has("dropItem")) def.setDropItem(obj.get("dropItem").getAsString());
            if (obj.has("dropChance")) def.setDropChance(obj.get("dropChance").getAsFloat());
            
            // Расширенная система дропа (DropRule)
            if (obj.has("drops")) {
                JsonArray dropsArr = obj.getAsJsonArray("drops");
                for (JsonElement dropEl : dropsArr) {
                    JsonObject dropObj = dropEl.getAsJsonObject();
                    String tool = dropObj.has("tool") ? dropObj.get("tool").getAsString() : "none";
                    String item = dropObj.get("item").getAsString();
                    float chance = dropObj.has("chance") ? dropObj.get("chance").getAsFloat() : 1.0f;
                    def.addDropRule(new com.za.minecraft.world.blocks.DropRule(tool, item, chance));
                }
            }

            if (obj.has("supportScavenge")) def.setSupportScavenge(obj.get("supportScavenge").getAsBoolean());
            if (obj.has("alwaysRender")) def.setAlwaysRender(obj.get("alwaysRender").getAsBoolean());
            if (obj.has("replaceable")) def.setReplaceable(obj.get("replaceable").getAsBoolean());
            
            if (obj.has("upperTexture")) {
                def.setUpperTexture("minecraft/textures/block/" + obj.get("upperTexture").getAsString());
            }

            if (obj.has("placement")) {
                def.setPlacementType(com.za.minecraft.world.blocks.PlacementType.valueOf(obj.get("placement").getAsString().toUpperCase()));
            }
            if (obj.has("shape")) {
                JsonArray shapeArr = obj.getAsJsonArray("shape");
                com.za.minecraft.world.physics.VoxelShape voxelShape = new com.za.minecraft.world.physics.VoxelShape();
                if (shapeArr.size() > 0) {
                    for (JsonElement boxEl : shapeArr) {
                        JsonArray boxCoords = boxEl.getAsJsonArray();
                        float minX = boxCoords.get(0).getAsFloat();
                        float minY = boxCoords.get(1).getAsFloat();
                        float minZ = boxCoords.get(2).getAsFloat();
                        float maxX = boxCoords.get(3).getAsFloat();
                        float maxY = boxCoords.get(4).getAsFloat();
                        float maxZ = boxCoords.get(5).getAsFloat();
                        voxelShape.addBox(new com.za.minecraft.world.physics.AABB(minX, minY, minZ, maxX, maxY, maxZ));
                    }
                    def.setShape(voxelShape);
                } else {
                    // Пустой массив - полное отсутствие хитбоксов (даже для выделения)
                    def.setShape(voxelShape);
                    def.setFullCube(false);
                }
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
            
            // Если форма не задана явно, и блок прозрачный - пробуем автогенерацию
            if (!obj.has("shape") && transparent) {
                def.autoGenerateShape();
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

            Item item = ItemTypeRegistry.create(type, id, identifier, translationKey, texture);
            
            if (obj.has("weight")) item.setWeight(obj.get("weight").getAsFloat());
            if (obj.has("visualScale")) item.setVisualScale(obj.get("visualScale").getAsFloat());
            
            // Парсинг компонентов (перезаписывают дефолтные из конструктора)
            if (obj.has("components")) {
                JsonObject comps = obj.getAsJsonObject("components");
                
                if (comps.has("minecraft:food") || comps.has("food")) {
                    JsonObject f = comps.has("minecraft:food") ? comps.getAsJsonObject("minecraft:food") : comps.getAsJsonObject("food");
                    item.addComponent(FoodComponent.class, new FoodComponent(
                        f.get("nutrition").getAsFloat(),
                        f.get("saturation").getAsFloat()
                    ));
                }
                
                if (comps.has("minecraft:tool") || comps.has("tool")) {
                    JsonObject t = comps.has("minecraft:tool") ? comps.getAsJsonObject("minecraft:tool") : comps.getAsJsonObject("tool");
                    item.addComponent(ToolComponent.class, new ToolComponent(
                        ToolType.valueOf(t.get("type").getAsString().toUpperCase()),
                        t.get("efficiency").getAsFloat(),
                        t.get("durability").getAsInt(),
                        t.has("isEffectiveAgainstAll") && t.get("isEffectiveAgainstAll").getAsBoolean()
                    ));
                }

                if (comps.has("minecraft:fuel") || comps.has("fuel")) {
                    JsonObject f = comps.has("minecraft:fuel") ? comps.getAsJsonObject("minecraft:fuel") : comps.getAsJsonObject("fuel");
                    item.addComponent(com.za.minecraft.world.items.component.FuelComponent.class, new com.za.minecraft.world.items.component.FuelComponent(
                        f.get("fuelAmount").getAsFloat()
                    ));
                }
                
                if (comps.has("minecraft:bag") || comps.has("bag")) {
                    JsonObject b = comps.has("minecraft:bag") ? comps.getAsJsonObject("minecraft:bag") : comps.getAsJsonObject("bag");
                    item.addComponent(com.za.minecraft.world.items.component.BagComponent.class, new com.za.minecraft.world.items.component.BagComponent(
                        b.get("slots").getAsInt()
                    ));
                }
                
                if (comps.has("minecraft:equipment") || comps.has("equipment")) {
                    JsonObject e = comps.has("minecraft:equipment") ? comps.getAsJsonObject("minecraft:equipment") : comps.getAsJsonObject("equipment");
                    item.addComponent(com.za.minecraft.world.items.component.EquipmentComponent.class, new com.za.minecraft.world.items.component.EquipmentComponent(
                        e.get("slot").getAsString()
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
                    obj.has("toolType") ? ToolType.valueOf(obj.get("toolType").getAsString().toUpperCase()) : ToolType.NONE,
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
        String indexPath = path + "/.index";
        try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(indexPath)) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        String cleanName = line.trim();
                        if (cleanName.startsWith("\uFEFF")) {
                            cleanName = cleanName.substring(1); // Удаляем BOM если есть
                        }
                        files.add(cleanName);
                    }
                }
            } else {
                Logger.warn("Index file not found in resources: " + indexPath);
            }
        } catch (Exception e) {
            Logger.warn("Could not read index for " + path + ": " + e.getMessage());
        }
        return files;
    }
}
