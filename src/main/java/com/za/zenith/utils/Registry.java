package com.za.zenith.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Универсальный реестр для объектов типа T, идентифицируемых по Identifier.
 */
public class Registry<T> {
    private final Map<Identifier, T> identifierMap = new HashMap<>();
    private final T defaultValue;

    public Registry() {
        this(null);
    }

    public Registry(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void register(Identifier id, T value) {
        identifierMap.put(id, value);
    }

    public T get(Identifier id) {
        return identifierMap.getOrDefault(id, defaultValue);
    }

    public T get(String id) {
        return get(Identifier.of(id));
    }

    public Set<Identifier> getIds() {
        return identifierMap.keySet();
    }

    public Collection<T> values() {
        return identifierMap.values();
    }

    public boolean contains(Identifier id) {
        return identifierMap.containsKey(id);
    }
    
    public int size() {
        return identifierMap.size();
    }
}


