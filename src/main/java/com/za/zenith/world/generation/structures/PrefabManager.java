package com.za.zenith.world.generation.structures;

import com.za.zenith.utils.Identifier;

/**
 * Статические ссылки на шаблоны структур для быстрого доступа.
 * Инициализируются автоматически через рефлексию в DataLoader.
 */
public class PrefabManager {
    public static StructureTemplate RUINED_HOUSE_1;
    public static StructureTemplate SMALL_STORE;
    public static StructureTemplate APARTMENT_BUILDING;
    public static StructureTemplate SKYSCRAPER_FLOOR;
    public static StructureTemplate SKYSCRAPER_ROOF;

    public static void init() {
        for (java.lang.reflect.Field field : PrefabManager.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    Identifier id = Identifier.of("zenith", field.getName().toLowerCase());
                    StructureTemplate template = StructureRegistry.get(id);
                    if (template != null) {
                        field.set(null, template);
                    }
                } catch (Exception e) {
                    com.za.zenith.utils.Logger.error("Failed to auto-init prefab field: " + field.getName());
                }
            }
        }
    }
}
