package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.generation.structures.StructureRegistry;
import com.za.zenith.world.generation.structures.StructureTemplate;

import java.util.HashMap;
import java.util.Map;

public class StructureDataLoader extends AbstractJsonLoader<StructureTemplate> {

    public StructureDataLoader() {
        super("structures");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonObject obj = el.getAsJsonObject();
            Identifier id = Identifier.of(obj.get("identifier").getAsString());
            
            // Palette parsing
            JsonObject paletteObj = obj.getAsJsonObject("palette");
            Map<Character, Integer> palette = new HashMap<>();
            for (String key : paletteObj.keySet()) {
                char symbol = key.charAt(0);
                String blockIdStr = paletteObj.get(key).getAsString();
                if (blockIdStr.matches("-?\\d+")) {
                    palette.put(symbol, Integer.parseInt(blockIdStr));
                } else {
                    palette.put(symbol, BlockRegistry.getRegistry().getId(Identifier.of(blockIdStr)));
                }
            }

            // Layers parsing
            JsonArray layersArr = obj.getAsJsonArray("layers");
            String[][] layers = new String[layersArr.size()][];
            for (int y = 0; y < layersArr.size(); y++) {
                JsonArray rowArr = layersArr.get(y).getAsJsonArray();
                layers[y] = new String[rowArr.size()];
                for (int z = 0; z < rowArr.size(); z++) {
                    layers[y][z] = rowArr.get(z).getAsString();
                }
            }

            StructureTemplate template = StructureTemplate.parse(layers, palette);
            StructureRegistry.register(id, template);
        } catch (Exception e) {
            Logger.error("Failed to parse structure " + sourcePath + ": " + e.getMessage());
        }
    }
}