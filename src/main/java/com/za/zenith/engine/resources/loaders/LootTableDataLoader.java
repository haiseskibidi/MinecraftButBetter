package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.items.loot.LootTable;
import com.za.zenith.world.items.loot.LootTableRegistry;

import java.util.ArrayList;
import java.util.List;

public class LootTableDataLoader extends AbstractJsonLoader<LootTable> {

    public LootTableDataLoader() {
        super("registry/loot_tables");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonObject obj = el.getAsJsonObject();
            Identifier id = Identifier.of(obj.get("identifier").getAsString());
            
            List<LootTable.Pool> pools = new ArrayList<>();
            JsonArray poolsArr = obj.getAsJsonArray("pools");
            for (JsonElement poolEl : poolsArr) {
                JsonObject p = poolEl.getAsJsonObject();
                int rolls = p.has("rolls") ? p.get("rolls").getAsInt() : 1;
                List<LootTable.Entry> entries = new ArrayList<>();
                JsonArray entriesArr = p.getAsJsonArray("entries");
                for (JsonElement entryEl : entriesArr) {
                    JsonObject e = entryEl.getAsJsonObject();
                    entries.add(new LootTable.Entry(
                        Identifier.of(e.get("item").getAsString()),
                        e.has("weight") ? e.get("weight").getAsInt() : 1
                    ));
                }
                pools.add(new LootTable.Pool(rolls, entries));
            }
            LootTable table = new LootTable(id, pools);
            table.setSourcePath(sourcePath);
            LootTableRegistry.register(table);
        } catch (Exception e) {
            Logger.error("Failed to parse loot table " + sourcePath + ": " + e.getMessage());
        }
    }
}