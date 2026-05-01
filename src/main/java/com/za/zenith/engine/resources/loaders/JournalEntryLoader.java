package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.journal.*;

import java.util.ArrayList;
import java.util.List;

public class JournalEntryLoader extends AbstractJsonLoader<JournalEntry> {

    public JournalEntryLoader() {
        super("journal/entries");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            JsonObject obj = el.getAsJsonObject();
            
            String idStr = obj.has("identifier") ? obj.get("identifier").getAsString() : obj.get("id").getAsString();
            Identifier id = Identifier.of(idStr);
            String title = obj.get("title").getAsString();
            Identifier icon = obj.has("icon") ? Identifier.of(obj.get("icon").getAsString()) : null;
            
            List<JournalElement> elements = new ArrayList<>();
            if (obj.has("elements")) {
                JsonArray arr = obj.getAsJsonArray("elements");
                for (JsonElement e : arr) {
                    JsonObject elObj = e.getAsJsonObject();
                    String typeStr = elObj.get("type").getAsString().toUpperCase();
                    JournalElement.Type type = JournalElement.Type.valueOf(typeStr);
                    
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
                    
                    elements.add(new JournalElement(type, value, imgPath, scale, items, color, align));
                }
            }
            
            JournalEntry entry = new JournalEntry(id, title, icon, elements);
            JournalRegistry.registerEntry(entry);
        } catch (Exception e) {
            Logger.error("Failed to parse journal entry " + sourcePath + ": " + e.getMessage());
        }
    }
}