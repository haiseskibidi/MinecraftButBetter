package com.za.zenith.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр, который автоматически назначает числовые ID объектам.
 */
public class NumericalRegistry<T> extends Registry<T> {
    private final Map<Integer, Identifier> idToIdentifier = new HashMap<>();
    private final Map<Identifier, Integer> identifierToId = new HashMap<>();
    private int nextId = 0;

    public NumericalRegistry() {
        super();
    }

    public NumericalRegistry(T defaultValue) {
        super(defaultValue);
    }

    /**
     * Регистрирует объект с автоматическим назначением следующего свободного ID.
     */
    @Override
    public void register(Identifier id, T value) {
        register(id, nextId++, value);
    }

    /**
     * Регистрирует объект с конкретным ID.
     */
    public void register(Identifier id, int numericalId, T value) {
        super.register(id, value);
        idToIdentifier.put(numericalId, id);
        identifierToId.put(id, numericalId);
        if (numericalId >= nextId) {
            nextId = numericalId + 1;
        }
    }

    public int getId(Identifier id) {
        return identifierToId.getOrDefault(id, -1);
    }

    public Identifier getIdentifier(int numericalId) {
        return idToIdentifier.get(numericalId);
    }
    
    public T get(int numericalId) {
        Identifier id = getIdentifier(numericalId);
        return id != null ? get(id) : null;
    }
}
