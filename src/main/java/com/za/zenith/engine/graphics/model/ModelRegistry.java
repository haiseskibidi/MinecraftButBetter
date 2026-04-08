package com.za.zenith.engine.graphics.model;

import com.za.zenith.utils.Identifier;
import java.util.HashMap;
import java.util.Map;

public class ModelRegistry {
    private static final Map<Identifier, ViewmodelDefinition> viewmodels = new HashMap<>();

    public static void registerViewmodel(Identifier id, ViewmodelDefinition def) {
        viewmodels.put(id, def);
    }

    public static ViewmodelDefinition getViewmodel(Identifier id) {
        return viewmodels.get(id);
    }

    public static java.util.Collection<ViewmodelDefinition> getAllViewmodels() {
        return viewmodels.values();
    }
}


