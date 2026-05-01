package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.journal.JournalCategory;
import com.za.zenith.world.journal.JournalRegistry;

import java.util.ArrayList;
import java.util.List;

public class JournalCategoryLoader extends AbstractJsonLoader<JournalCategory> {

    public JournalCategoryLoader() {
        super("journal/categories");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonObject obj = el.getAsJsonObject();
            
            String idStr = obj.has("identifier") ? obj.get("identifier").getAsString() : obj.get("id").getAsString();
            Identifier id = Identifier.of(idStr);
            String name = obj.get("name").getAsString();
            Identifier icon = Identifier.of(obj.get("icon").getAsString());
            
            List<Identifier> entries = new ArrayList<>();
            if (obj.has("entries")) {
                JsonArray arr = obj.getAsJsonArray("entries");
                for (JsonElement e : arr) {
                    entries.add(Identifier.of(e.getAsString()));
                }
            }
            
            JournalCategory category = new JournalCategory(id, name, icon, entries);
            JournalRegistry.registerCategory(category);
        } catch (Exception e) {
            Logger.error("Failed to parse journal category " + sourcePath + ": " + e.getMessage());
        }
    }
}