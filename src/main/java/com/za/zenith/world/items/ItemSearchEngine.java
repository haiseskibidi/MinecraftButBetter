package com.za.zenith.world.items;

import com.za.zenith.utils.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Универсальный движок поиска предметов.
 * Поддерживает поиск по локализованному названию, пути идентификатора и полному идентификатору.
 */
public class ItemSearchEngine {
    
    /**
     * Фильтрует список предметов на основе поискового запроса.
     * 
     * @param items Исходный список предметов.
     * @param query Поисковый запрос.
     * @return Отфильтрованный список.
     */
    public static List<Item> filter(List<Item> items, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(items);
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        return items.stream()
            .filter(item -> matches(item, lowerQuery))
            .collect(Collectors.toList());
    }
    
    /**
     * Проверяет, соответствует ли предмет запросу.
     */
    private static boolean matches(Item item, String query) {
        // 1. Поиск по переведенному названию (например, "камень")
        String translatedName = I18n.get(item.getName()).toLowerCase();
        if (translatedName.contains(query)) return true;
        
        // 2. Поиск по пути идентификатора (английское название, например, "stone")
        String path = item.getIdentifier().getPath().toLowerCase();
        if (path.contains(query)) return true;
        
        // 3. Поиск по полному идентификатору (например, "zenith:stone")
        String fullId = item.getIdentifier().toString().toLowerCase();
        if (fullId.contains(query)) return true;
        
        return false;
    }
}
