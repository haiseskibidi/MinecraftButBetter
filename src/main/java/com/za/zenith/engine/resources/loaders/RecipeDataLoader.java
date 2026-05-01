package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.ItemRegistry;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.recipes.*;

import java.util.ArrayList;
import java.util.List;

public class RecipeDataLoader extends AbstractJsonLoader<IRecipe> {

    public RecipeDataLoader() {
        super("recipes");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
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
                Item resItem = ItemRegistry.getItem(resId);

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
                IRecipe recipe = new NappingRecipe(id, inputs, result, pattern);
                if (recipe instanceof com.za.zenith.utils.LiveReloadable lr) lr.setSourcePath(sourcePath);
                RecipeRegistry.register(recipe);
            }
            else if (type.equalsIgnoreCase("stump_crafting")) {
                Identifier input = Identifier.of(obj.get("input").getAsString());
                Identifier tool = obj.has("tool") ? Identifier.of(obj.get("tool").getAsString()) : null;
                int hits = obj.get("hits").getAsInt();
                
                JsonObject resObj = obj.getAsJsonObject("result");
                Identifier resId = Identifier.of(resObj.get("item").getAsString());
                int count = resObj.has("count") ? resObj.get("count").getAsInt() : 1;
                
                Item resItem = ItemRegistry.getItem(resId);
                if (resItem != null) {
                    IRecipe recipe = new StumpRecipe(id, input, tool, hits, new ItemStack(resItem, count));
                    if (recipe instanceof com.za.zenith.utils.LiveReloadable lr) lr.setSourcePath(sourcePath);
                    RecipeRegistry.register(recipe);
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
                Identifier requiredSurface = obj.has("required_surface") ? Identifier.of(obj.get("required_surface").getAsString()) : null;
                int hits = obj.get("hits").getAsInt();

                JsonObject resObj = obj.getAsJsonObject("result");
                Identifier resId = Identifier.of(resObj.get("item").getAsString());
                int count = resObj.has("count") ? resObj.get("count").getAsInt() : 1;

                Item resItem = ItemRegistry.getItem(resId);
                if (resItem != null) {
                    IRecipe recipe = new InWorldRecipe(id, ingredients, tool, requiredSurface, hits, new ItemStack(resItem, count));
                    if (recipe instanceof com.za.zenith.utils.LiveReloadable lr) lr.setSourcePath(sourcePath);
                    RecipeRegistry.register(recipe);
                } else {
                    Logger.error("Failed to parse in_world recipe " + id + ": Result item " + resId + " not found!");
                }
            } else if (type.equalsIgnoreCase("carving")) {
                Identifier input = Identifier.of(obj.get("input").getAsString());
                Identifier tool = obj.has("tool") ? Identifier.of(obj.get("tool").getAsString()) : null;
                Identifier intermediate = Identifier.of(obj.get("intermediate").getAsString());
                Identifier result = Identifier.of(obj.get("result").getAsString());
                
                IRecipe recipe = new CarvingRecipe(id, input, tool, intermediate, result);
                if (recipe instanceof com.za.zenith.utils.LiveReloadable lr) lr.setSourcePath(sourcePath);
                RecipeRegistry.register(recipe);
            }
        } catch (Exception e) {
            Logger.error("Failed to parse recipe " + sourcePath + ": " + e.getMessage());
        }
    }
}