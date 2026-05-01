package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.ItemRegistry;
import com.za.zenith.world.items.ItemTypeRegistry;
import com.za.zenith.world.items.ToolType;
import com.za.zenith.world.items.component.FoodComponent;
import com.za.zenith.world.items.component.ToolComponent;

public class ItemDataLoader extends AbstractJsonLoader<Item> {

    public ItemDataLoader() {
        super("items");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonObject obj = el.getAsJsonObject();
            Identifier identifier = Identifier.of(obj.get("identifier").getAsString());
            
            int id;
            if (obj.has("id")) {
                id = obj.get("id").getAsInt();
            } else {
                int existingId = ItemRegistry.getRegistry().getId(identifier);
                if (existingId != -1) {
                    id = existingId;
                } else {
                    id = ItemRegistry.getRegistry().getNextAvailableId();
                }
            }
            
            String translationKey = obj.get("translationKey").getAsString();
            String texture = obj.get("texture").getAsString();
            if (texture.startsWith("zenith:")) {
                texture = texture.replace("zenith:", "zenith/");
            }
            String type = obj.has("type") ? obj.get("type").getAsString() : "default";

            Item item = ItemTypeRegistry.create(type, id, identifier, translationKey, texture);
            item.setSourcePath(sourcePath);
            
            if (obj.has("weight")) item.setWeight(obj.get("weight").getAsFloat());
            if (obj.has("visualScale")) item.setDroppedScale(obj.get("visualScale").getAsFloat()); // Legacy support
            if (obj.has("droppedScale")) item.setDroppedScale(obj.get("droppedScale").getAsFloat());
            if (obj.has("viewmodelScale")) item.setViewmodelScale(obj.get("viewmodelScale").getAsFloat());
            if (obj.has("lightLevel")) item.setLightLevel(obj.get("lightLevel").getAsInt());
            if (obj.has("miningSpeed")) item.setMiningSpeed(obj.get("miningSpeed").getAsFloat());
            if (obj.has("maxStackSize")) item.setMaxStackSize(obj.get("maxStackSize").getAsInt());
            if (obj.has("interaction_cooldown")) item.setInteractionCooldown(obj.get("interaction_cooldown").getAsFloat());
            if (obj.has("gender")) item.setGender(Item.Gender.valueOf(obj.get("gender").getAsString().toUpperCase()));
            if (obj.has("rarity")) item.setDefaultRarity(Identifier.of(obj.get("rarity").getAsString()));
            if (obj.has("description")) item.setDescriptionKey(obj.get("description").getAsString());

            if (obj.has("tags") && obj.get("tags").isJsonArray()) {
                JsonArray tagsArr = obj.get("tags").getAsJsonArray();
                for (JsonElement elTag : tagsArr) {
                    item.addTag(Identifier.of(elTag.getAsString()));
                }
            }
            
            // --- HOT RELOAD FIX: Clear existing components ---
            try {
                java.lang.reflect.Field field = com.za.zenith.world.items.Item.class.getDeclaredField("components");
                field.setAccessible(true);
                ((java.util.Map<?, ?>) field.get(item)).clear();
            } catch (Exception e) {
                Logger.error("Failed to clear components for hot-reload: " + e.getMessage());
            }

            // Парсинг компонентов (перезаписывают дефолтные из конструктора)
            if (obj.has("components")) {
                JsonObject comps = obj.getAsJsonObject("components");
                
                if (comps.has("zenith:lootbox") || comps.has("lootbox")) {
                    JsonObject l = comps.has("zenith:lootbox") ? comps.getAsJsonObject("zenith:lootbox") : comps.getAsJsonObject("lootbox");
                    java.util.Map<Identifier, Integer> rW = new java.util.HashMap<>();
                    if (l.has("rarity_weights")) {
                        for (java.util.Map.Entry<String, JsonElement> e : l.getAsJsonObject("rarity_weights").entrySet()) {
                            rW.put(Identifier.of(e.getKey()), e.getValue().getAsInt());
                        }
                    }
                    java.util.Map<Identifier, Integer> aW = new java.util.HashMap<>();
                    if (l.has("affix_weights")) {
                        for (java.util.Map.Entry<String, JsonElement> e : l.getAsJsonObject("affix_weights").entrySet()) {
                            aW.put(Identifier.of(e.getKey()), e.getValue().getAsInt());
                        }
                    }
                    item.addComponent(com.za.zenith.world.items.component.LootboxComponent.class, new com.za.zenith.world.items.component.LootboxComponent(
                        Identifier.of(l.get("loot_table").getAsString()),
                        l.has("opening_time") ? l.get("opening_time").getAsFloat() : 1.0f,
                        rW.isEmpty() ? null : rW,
                        aW.isEmpty() ? null : aW
                    ));
                }

                if (comps.has("zenith:food") || comps.has("food")) {
                    JsonObject f = comps.has("zenith:food") ? comps.getAsJsonObject("zenith:food") : comps.getAsJsonObject("food");
                    item.addComponent(FoodComponent.class, new FoodComponent(
                        f.get("nutrition").getAsFloat(),
                        f.get("saturation").getAsFloat()
                    ));
                }
                
                if (comps.has("zenith:magnetic") || comps.has("magnetic")) {
                    JsonObject m = comps.has("zenith:magnetic") ? comps.getAsJsonObject("zenith:magnetic") : comps.getAsJsonObject("magnetic");
                    item.addComponent(com.za.zenith.world.items.component.MagneticComponent.class, new com.za.zenith.world.items.component.MagneticComponent(
                        m.has("attractionRadius") ? m.get("attractionRadius").getAsFloat() : 4.0f,
                        m.has("pickupRadius") ? m.get("pickupRadius").getAsFloat() : 0.2f,
                        m.has("attractionForce") ? m.get("attractionForce").getAsFloat() : 45.0f
                    ));
                }

                if (comps.has("zenith:tool") || comps.has("tool")) {
                    JsonObject t = comps.has("zenith:tool") ? comps.getAsJsonObject("zenith:tool") : comps.getAsJsonObject("tool");

                    int durability = 100;
                    if (t.has("durability")) {
                        JsonElement durEl = t.get("durability");
                        if (durEl.isJsonPrimitive() && durEl.getAsJsonPrimitive().isString()) {
                            String durStr = durEl.getAsString();
                            if (durStr.equalsIgnoreCase("inf")) durability = -1;
                            else durability = (int)Double.parseDouble(durStr);
                        } else {
                            durability = durEl.getAsInt();
                        }
                    }

                    item.addComponent(com.za.zenith.world.items.component.ToolComponent.class, new com.za.zenith.world.items.component.ToolComponent(
                        com.za.zenith.world.items.ToolType.valueOf(t.get("type").getAsString().toUpperCase()),
                        t.get("efficiency").getAsFloat(),
                        durability,
                        t.has("isEffectiveAgainstAll") && t.get("isEffectiveAgainstAll").getAsBoolean(),
                        t.has("attackInterval") ? t.get("attackInterval").getAsFloat() : 0.5f
                    ));
                }

                if (comps.has("zenith:viewmodel") || comps.has("viewmodel")) {
                    JsonObject v = comps.has("zenith:viewmodel") ? comps.getAsJsonObject("zenith:viewmodel") : comps.getAsJsonObject("viewmodel");
                    String socket = v.has("socket") ? v.get("socket").getAsString() : null;
                    float[] translation = v.has("translation") ? com.za.zenith.engine.resources.AssetManager.getGson().fromJson(v.getAsJsonArray("translation"), float[].class) : new float[]{0, 0, 0};
                    float[] rotation = v.has("rotation") ? com.za.zenith.engine.resources.AssetManager.getGson().fromJson(v.getAsJsonArray("rotation"), float[].class) : new float[]{0, 0, 0};
                    float scale = v.has("scale") ? v.get("scale").getAsFloat() : 1.0f;
                    
                    com.za.zenith.engine.graphics.model.GripDefinition grip = null;
                    if (v.has("grip")) {
                        JsonElement gripEl = v.get("grip");
                        if (gripEl.isJsonPrimitive()) {
                            grip = com.za.zenith.engine.graphics.model.GripRegistry.get(gripEl.getAsString());
                        } else if (gripEl.isJsonObject()) {
                            grip = com.za.zenith.engine.resources.AssetManager.getGson().fromJson(gripEl, com.za.zenith.engine.graphics.model.GripDefinition.class);
                        }
                    }
                    
                    item.addComponent(com.za.zenith.world.items.component.ViewmodelComponent.class, 
                        new com.za.zenith.world.items.component.ViewmodelComponent(socket, translation, rotation, scale, grip));
                    
                    item.setViewmodelTransform(new Item.ViewmodelTransform(
                        translation[0], translation[1], translation[2],
                        rotation[0], rotation[1], rotation[2],
                        scale
                    ));
                }

                if (comps.has("zenith:fuel") || comps.has("fuel")) {
                    JsonObject f = comps.has("zenith:fuel") ? comps.getAsJsonObject("zenith:fuel") : comps.getAsJsonObject("fuel");
                    item.addComponent(com.za.zenith.world.items.component.FuelComponent.class, new com.za.zenith.world.items.component.FuelComponent(
                        f.get("fuelAmount").getAsFloat()
                    ));
                }
                
                if (comps.has("zenith:bag") || comps.has("bag")) {
                    JsonObject b = comps.has("zenith:bag") ? comps.getAsJsonObject("zenith:bag") : comps.getAsJsonObject("bag");
                    item.addComponent(com.za.zenith.world.items.component.BagComponent.class, new com.za.zenith.world.items.component.BagComponent(
                        b.get("slots").getAsInt()
                    ));
                }
                
                if (comps.has("zenith:equipment") || comps.has("equipment")) {
                    JsonObject e = comps.has("zenith:equipment") ? comps.getAsJsonObject("zenith:equipment") : comps.getAsJsonObject("equipment");
                    item.addComponent(com.za.zenith.world.items.component.EquipmentComponent.class, new com.za.zenith.world.items.component.EquipmentComponent(
                        e.get("slot").getAsString(),
                        e.has("strict") && e.get("strict").getAsBoolean()
                    ));
                }

                if (comps.has("zenith:animations") || comps.has("animations")) {
                    JsonObject anims = comps.has("zenith:animations") ? comps.getAsJsonObject("zenith:animations") : comps.getAsJsonObject("animations");
                    java.util.Map<String, String> overrides = new java.util.HashMap<>();
                    for (String key : anims.keySet()) {
                        overrides.put(key, anims.get(key).getAsString());
                    }
                    item.addComponent(com.za.zenith.world.items.component.AnimationComponent.class, new com.za.zenith.world.items.component.AnimationComponent(overrides));
                }

                if (comps.has("zenith:thermal") || comps.has("thermal")) {
                    JsonObject t = comps.has("zenith:thermal") ? comps.getAsJsonObject("zenith:thermal") : comps.getAsJsonObject("thermal");
                    item.addComponent(com.za.zenith.world.items.component.ThermalComponent.class, new com.za.zenith.world.items.component.ThermalComponent(
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

            if (obj.has("light")) {
                JsonObject lObj = obj.getAsJsonObject("light");
                com.za.zenith.world.lighting.LightData ld = new com.za.zenith.world.lighting.LightData();
                if (lObj.has("type")) ld.type = com.za.zenith.world.lighting.LightData.parseType(lObj.get("type").getAsString());
                if (lObj.has("color")) {
                    JsonArray c = lObj.getAsJsonArray("color");
                    ld.color.set(c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat());
                }
                if (lObj.has("intensity")) ld.intensity = lObj.get("intensity").getAsFloat();
                if (lObj.has("radius")) ld.radius = lObj.get("radius").getAsFloat();
                if (lObj.has("spotAngle")) ld.spotAngle = lObj.get("spotAngle").getAsFloat();
                if (lObj.has("flicker")) ld.flicker = lObj.get("flicker").getAsBoolean();
                if (lObj.has("dynamic")) ld.dynamic = lObj.get("dynamic").getAsBoolean();
                item.setLightData(ld);
            }

            ItemRegistry.registerItem(item);
        } catch (Exception e) {
            Logger.error("Failed to parse item " + sourcePath + ": " + e.getMessage());
        }
    }
}