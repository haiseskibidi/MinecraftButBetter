package com.za.minecraft.world;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.utils.Logger;
import com.za.minecraft.world.blocks.BlockDefinition;
import com.za.minecraft.world.blocks.MiningSettings;
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
        loadWoodTypes();
        com.za.minecraft.utils.events.RegistryEvents.fireBlockRegistration();
        
        for (String ns : namespaces) {
            loadItems(ns);
        }
        com.za.minecraft.utils.events.RegistryEvents.fireItemRegistration();
        
        // --- Essential Initialization Order ---
        // 1. Map blocks to items automatically
        com.za.minecraft.world.items.ItemRegistry.init();
        // 2. Fill static holder classes via reflection
        com.za.minecraft.world.blocks.Blocks.init();
        com.za.minecraft.world.items.Items.init();
        // 3. Register structures (depends on blocks)
        for (String ns : namespaces) {
            loadStructures(ns);
        }
        com.za.minecraft.world.generation.structures.PrefabManager.init();

        for (String ns : namespaces) {
            loadEntityDefinitions(ns);
        }

        for (String ns : namespaces) {
            loadRecipes(ns);
        }

        for (String ns : namespaces) {
            loadGUIs(ns);
        }

        for (String ns : namespaces) {
            loadJournal(ns);
        }

        for (String ns : namespaces) {
            loadModels(ns);
            loadActions(ns);
        }

        loadParkourAnimations();
        loadScavengeSettings();
        loadPhysicsSettings();
    }

    private static void loadActions(String namespace) {
        List<String> actionNames = listResources(namespace + "/actions");
        for (String name : actionNames) {
            String path = namespace + "/actions/" + name + ".json";
            try (java.io.InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    com.za.minecraft.utils.Logger.warn("Action file not found: " + path);
                    continue;
                }
                com.za.minecraft.world.actions.ActionDefinition def = GSON.fromJson(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8), com.za.minecraft.world.actions.ActionDefinition.class);
                com.za.minecraft.world.actions.ActionRegistry.register(com.za.minecraft.utils.Identifier.of(namespace + ":" + name), def);
            } catch (Exception e) {
                com.za.minecraft.utils.Logger.error("Failed to load action " + path + ": " + e.getMessage());
            }
        }
    }

    private static void loadParkourAnimations() {
        List<String> files = listResources("minecraft/animations");
        for (String fileName : files) {
            String path = "minecraft/animations/" + fileName + ".json";
            try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(path)) {
                if (is == null) continue;
                JsonObject animObj = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                com.za.minecraft.entities.parkour.animation.AnimationProfile anim = new com.za.minecraft.entities.parkour.animation.AnimationProfile(fileName);
                
                if (animObj.has("duration")) anim.setDuration(animObj.get("duration").getAsFloat());
                if (animObj.has("duration_key")) anim.setDurationKey(animObj.get("duration_key").getAsString());
                if (animObj.has("looping")) anim.setLooping(animObj.get("looping").getAsBoolean());
                
                if (animObj.has("path")) {
                    JsonObject p = animObj.getAsJsonObject("path");
                    if (p.has("type")) anim.setPathType(p.get("type").getAsString());
                    if (p.has("interpolation")) anim.setPathInterpolation(p.get("interpolation").getAsString());
                    if (p.has("apex_y_offset")) anim.setApexYOffset(p.get("apex_y_offset").getAsFloat());
                }
                
                if (animObj.has("jitter")) {
                    JsonObject j = animObj.getAsJsonObject("jitter");
                    anim.setJitterEnabled(j.get("enabled").getAsBoolean());
                    anim.setJitterStart(j.get("start").getAsFloat());
                    anim.setJitterEnd(j.get("end").getAsFloat());
                    anim.setJitterIntensity(j.get("intensity").getAsFloat());
                }
                
                if (animObj.has("tracks")) {
                    JsonObject tracks = animObj.getAsJsonObject("tracks");
                    for (String trackKey : tracks.keySet()) {
                        com.za.minecraft.entities.parkour.animation.AnimationTrack track = new com.za.minecraft.entities.parkour.animation.AnimationTrack();
                        JsonArray keys;
                        
                        if (tracks.get(trackKey).isJsonObject()) {
                            JsonObject tObj = tracks.getAsJsonObject(trackKey);
                            if (tObj.has("mirror")) track.setMirror(tObj.get("mirror").getAsBoolean());
                            keys = tObj.getAsJsonArray("keyframes");
                        } else {
                            keys = tracks.getAsJsonArray(trackKey);
                        }
                        
                        for (com.google.gson.JsonElement ke : keys) {
                            JsonArray ka = ke.getAsJsonArray();
                            track.addKeyframe(new com.za.minecraft.entities.parkour.animation.Keyframe(
                                ka.get(0).getAsFloat(),
                                ka.get(1).getAsFloat(),
                                ka.get(2).getAsString()
                            ));
                        }
                        anim.addTrack(trackKey, track);
                    }
                }
                com.za.minecraft.entities.parkour.animation.AnimationRegistry.register(fileName, anim);
            } catch (Exception e) {
                Logger.error("Failed to load animation " + fileName + ": " + e.getMessage());
            }
        }
        Logger.info("Loaded " + files.size() + " animation profiles");
    }

    private static void loadPhysicsSettings() {
        try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream("minecraft/registry/physics.json")) {
            if (is == null) return;
            com.za.minecraft.world.physics.PhysicsSettings settings = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), com.za.minecraft.world.physics.PhysicsSettings.class);
            if (settings != null) {
                com.za.minecraft.world.physics.PhysicsSettings.setInstance(settings);
                Logger.info("Loaded physics settings");
            }
        } catch (Exception e) {
            Logger.error("Failed to load physics settings: " + e.getMessage());
        }
    }

    public static JsonObject loadJson(String path) {
        try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) return new JsonObject();
            return GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
        } catch (Exception e) {
            Logger.error("Failed to load JSON from " + path + ": " + e.getMessage());
            return new JsonObject();
        }
    }

    private static void loadJournal(String ns) {
        loadJournalCategories(ns);
        loadJournalEntries(ns);
    }

    private static void loadJournalCategories(String namespace) {
        String path = namespace + "/journal/categories";
        List<String> files = listResources(path);
        for (String file : files) {
            try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(path + "/" + file)) {
                if (is == null) continue;
                String json = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                JsonObject obj = GSON.fromJson(json, JsonObject.class);
                
                Identifier id = Identifier.of(obj.get("id").getAsString());
                String name = obj.get("name").getAsString();
                Identifier icon = Identifier.of(obj.get("icon").getAsString());
                
                List<Identifier> entries = new ArrayList<>();
                if (obj.has("entries")) {
                    JsonArray arr = obj.getAsJsonArray("entries");
                    for (JsonElement e : arr) {
                        entries.add(Identifier.of(e.getAsString()));
                    }
                }
                
                com.za.minecraft.world.journal.JournalCategory category = new com.za.minecraft.world.journal.JournalCategory(id, name, icon, entries);
                com.za.minecraft.world.journal.JournalRegistry.registerCategory(category);
            } catch (Exception e) {
                Logger.error("Failed to load journal category " + file + ": " + e.getMessage());
            }
        }
    }

    private static void loadJournalEntries(String namespace) {
        String path = namespace + "/journal/entries";
        List<String> files = listResources(path);
        for (String file : files) {
            try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(path + "/" + file)) {
                if (is == null) continue;
                String json = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                JsonObject obj = GSON.fromJson(json, JsonObject.class);
                
                Identifier id = Identifier.of(obj.get("id").getAsString());
                String title = obj.get("title").getAsString();
                Identifier icon = obj.has("icon") ? Identifier.of(obj.get("icon").getAsString()) : null;
                
                List<com.za.minecraft.world.journal.JournalElement> elements = new ArrayList<>();
                if (obj.has("elements")) {
                    JsonArray arr = obj.getAsJsonArray("elements");
                    for (JsonElement e : arr) {
                        JsonObject elObj = e.getAsJsonObject();
                        String typeStr = elObj.get("type").getAsString().toUpperCase();
                        com.za.minecraft.world.journal.JournalElement.Type type = com.za.minecraft.world.journal.JournalElement.Type.valueOf(typeStr);
                        
                        String value = elObj.has("value") ? elObj.get("value").getAsString() : null;
                        String imgPath = elObj.has("path") ? elObj.get("path").getAsString() : null;
                        float scale = elObj.has("scale") ? elObj.get("scale").getAsFloat() : 1.0f;
                        String color = elObj.has("color") ? elObj.get("color").getAsString() : null;
                        String align = elObj.has("alignment") ? elObj.get("alignment").getAsString() : "left";
                        
                        List<Identifier> items = null;
                        if (elObj.has("items")) {
                            items = new ArrayList<>();
                            JsonArray itemArr = elObj.getAsJsonArray("items");
                            for (JsonElement it : itemArr) {
                                items.add(Identifier.of(it.getAsString()));
                            }
                        }
                        
                        elements.add(new com.za.minecraft.world.journal.JournalElement(type, value, imgPath, scale, items, color, align));
                    }
                }
                
                com.za.minecraft.world.journal.JournalEntry entry = new com.za.minecraft.world.journal.JournalEntry(id, title, icon, elements);
                com.za.minecraft.world.journal.JournalRegistry.registerEntry(entry);
            } catch (Exception e) {
                Logger.error("Failed to load journal entry " + file + ": " + e.getMessage());
            }
        }
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

    private static void loadModels(String namespace) {
        String path = namespace + "/models/viewmodel";
        List<String> files = listResources(path);
        if (!files.isEmpty()) {
            for (String file : files) {
                try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream(path + "/" + file)) {
                    if (is == null) continue;
                    com.za.minecraft.engine.graphics.model.ViewmodelDefinition def = GSON.fromJson(
                        new InputStreamReader(is, StandardCharsets.UTF_8), 
                        com.za.minecraft.engine.graphics.model.ViewmodelDefinition.class
                    );
                    if (def != null) {
                        Identifier id = Identifier.of(namespace, file.replace(".json", ""));
                        com.za.minecraft.engine.graphics.model.ModelRegistry.registerViewmodel(id, def);
                        Logger.info("Loaded viewmodel: " + id);
                    }
                } catch (Exception e) {
                    Logger.error("Failed to load viewmodel " + file + ": " + e.getMessage());
                }
            }
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
                List<Identifier> inputs = new ArrayList<>();
                if (obj.get("input").isJsonArray()) {
                    for (JsonElement e : obj.getAsJsonArray("input")) inputs.add(Identifier.of(e.getAsString()));
                } else {
                    inputs.add(Identifier.of(obj.get("input").getAsString()));
                }

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
                RecipeRegistry.register(new NappingRecipe(id, inputs, result, pattern));
            }
 else if (type.equalsIgnoreCase("stump_crafting")) {
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
            } else if (type.equalsIgnoreCase("carving")) {
                Identifier input = Identifier.of(obj.get("input").getAsString());
                Identifier tool = obj.has("tool") ? Identifier.of(obj.get("tool").getAsString()) : null;
                Identifier intermediate = Identifier.of(obj.get("intermediate").getAsString());
                Identifier result = Identifier.of(obj.get("result").getAsString());
                
                RecipeRegistry.register(new com.za.minecraft.world.recipes.CarvingRecipe(id, input, tool, intermediate, result));
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

    private static void loadWoodTypes() {
        try (InputStream is = DataLoader.class.getClassLoader().getResourceAsStream("minecraft/registry/wood_types.json")) {
            if (is == null) return;
            JsonArray root = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
            List<Identifier> types = new ArrayList<>();
            for (JsonElement el : root) {
                types.add(Identifier.of(el.getAsString()));
            }
            com.za.minecraft.world.blocks.WoodTypeRegistry.init(types);
            Logger.info("Loaded " + types.size() + " wood types");
        } catch (Exception e) {
            Logger.error("Failed to load wood types: " + e.getMessage());
        }
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
            if (obj.has("fellingStages")) def.setFellingStages(obj.get("fellingStages").getAsInt());
            if (obj.has("nextStage")) def.setNextStage(Identifier.of(obj.get("nextStage").getAsString()));
            if (obj.has("next_stage")) def.setNextStage(Identifier.of(obj.get("next_stage").getAsString()));
            if (obj.has("soilingAmount")) def.setSoilingAmount(obj.get("soilingAmount").getAsFloat());
            if (obj.has("cleaningAmount")) def.setCleaningAmount(obj.get("cleaningAmount").getAsFloat());
            if (obj.has("firingTemperature")) def.setFiringTemperature(obj.get("firingTemperature").getAsFloat());
            if (obj.has("requiredTool")) def.setRequiredTool(obj.get("requiredTool").getAsString());            if (obj.has("dropItem")) def.setDropItem(obj.get("dropItem").getAsString());
            if (obj.has("dropChance")) def.setDropChance(obj.get("dropChance").getAsFloat());
            
            if (obj.has("wobble_animation")) def.setWobbleAnimation(obj.get("wobble_animation").getAsString());
            if (obj.has("interaction_cooldown")) def.setInteractionCooldown(obj.get("interaction_cooldown").getAsFloat());
            
            if (obj.has("breaking_pattern")) {
                String pattern = obj.get("breaking_pattern").getAsString().toLowerCase();
                int patternId = switch(pattern) {
                    case "wood" -> 1;
                    case "stone" -> 2;
                    case "shatter", "glass" -> 3;
                    default -> 0;
                };
                def.setBreakingPattern(patternId);
            }

            if (obj.has("mining_logic")) {
                JsonObject ml = obj.getAsJsonObject("mining_logic");
                String strategy = ml.has("strategy") ? ml.get("strategy").getAsString() : "default";
                float precision = ml.has("precision") ? ml.get("precision").getAsFloat() : 0.2f;
                float multiplier = ml.has("miss_multiplier") ? ml.get("miss_multiplier").getAsFloat() : 1.0f;
                
                org.joml.Vector3f wsColor = new org.joml.Vector3f(1.0f, 1.0f, 1.0f);
                if (ml.has("weak_spot_color")) {
                    JsonArray col = ml.getAsJsonArray("weak_spot_color");
                    wsColor.set(col.get(0).getAsFloat(), col.get(1).getAsFloat(), col.get(2).getAsFloat());
                }
                
                def.setMiningSettings(new MiningSettings(strategy, precision, multiplier, wsColor));
            }

            // Расширенная система дропа (DropRule)
            if (obj.has("drops")) {
                JsonArray dropsArr = obj.getAsJsonArray("drops");
                for (JsonElement dropEl : dropsArr) {
                    JsonObject dropObj = dropEl.getAsJsonObject();
                    String tool = dropObj.has("tool") ? dropObj.get("tool").getAsString() : "none";
                    String item = dropObj.get("item").getAsString();
                    float chance = dropObj.has("chance") ? dropObj.get("chance").getAsFloat() : 1.0f;
                    boolean dropOnHit = dropObj.has("drop_on_hit") && dropObj.get("drop_on_hit").getAsBoolean();
                    def.addDropRule(new com.za.minecraft.world.blocks.DropRule(tool, item, chance, dropOnHit));
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

            if (obj.has("tags")) {
                JsonArray tagsArr = obj.getAsJsonArray("tags");
                for (JsonElement tagEl : tagsArr) {
                    String tag = tagEl.getAsString();
                    def.addTag(tag);
                    if (tag.equals("minecraft:tinted")) {
                        def.setTinted(true);
                    }
                }
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
            if (obj.has("miningSpeed")) item.setMiningSpeed(obj.get("miningSpeed").getAsFloat());
            if (obj.has("maxStackSize")) item.setMaxStackSize(obj.get("maxStackSize").getAsInt());
            if (obj.has("interaction_cooldown")) item.setInteractionCooldown(obj.get("interaction_cooldown").getAsFloat());
            
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
                
                if (comps.has("minecraft:magnetic") || comps.has("magnetic")) {
                    JsonObject m = comps.has("minecraft:magnetic") ? comps.getAsJsonObject("minecraft:magnetic") : comps.getAsJsonObject("magnetic");
                    item.addComponent(com.za.minecraft.world.items.component.MagneticComponent.class, new com.za.minecraft.world.items.component.MagneticComponent(
                        m.has("attractionRadius") ? m.get("attractionRadius").getAsFloat() : 4.0f,
                        m.has("pickupRadius") ? m.get("pickupRadius").getAsFloat() : 0.2f,
                        m.has("attractionForce") ? m.get("attractionForce").getAsFloat() : 45.0f
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

                if (comps.has("minecraft:viewmodel") || comps.has("viewmodel")) {
                    JsonObject v = comps.has("minecraft:viewmodel") ? comps.getAsJsonObject("minecraft:viewmodel") : comps.getAsJsonObject("viewmodel");
                    JsonArray pos = v.getAsJsonArray("translation");
                    JsonArray rot = v.getAsJsonArray("rotation");
                    float scale = v.get("scale").getAsFloat();
                    item.setViewmodelTransform(new Item.ViewmodelTransform(
                        pos.get(0).getAsFloat(), pos.get(1).getAsFloat(), pos.get(2).getAsFloat(),
                        rot.get(0).getAsFloat(), rot.get(1).getAsFloat(), rot.get(2).getAsFloat(),
                        scale
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
                        e.get("slot").getAsString(),
                        e.has("strict") && e.get("strict").getAsBoolean()
                    ));
                }

                if (comps.has("minecraft:animations") || comps.has("animations")) {
                    JsonObject anims = comps.has("minecraft:animations") ? comps.getAsJsonObject("minecraft:animations") : comps.getAsJsonObject("animations");
                    java.util.Map<String, String> overrides = new java.util.HashMap<>();
                    for (String key : anims.keySet()) {
                        overrides.put(key, anims.get(key).getAsString());
                    }
                    item.addComponent(com.za.minecraft.world.items.component.AnimationComponent.class, new com.za.minecraft.world.items.component.AnimationComponent(overrides));
                }

                if (comps.has("minecraft:thermal") || comps.has("thermal")) {
                    JsonObject t = comps.has("minecraft:thermal") ? comps.getAsJsonObject("minecraft:thermal") : comps.getAsJsonObject("thermal");
                    item.addComponent(com.za.minecraft.world.items.component.ThermalComponent.class, new com.za.minecraft.world.items.component.ThermalComponent(
                        t.has("initialTemperature") ? t.get("initialTemperature").getAsFloat() : 20.0f,
                        t.has("specificHeatCapacity") ? t.get("specificHeatCapacity").getAsFloat() : 0.05f,
                        t.has("burnThreshold") ? t.get("burnThreshold").getAsFloat() : 55.0f
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
