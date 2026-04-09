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
        register(id, getNextAvailableId(), value);
    }

    /**
     * Регистрирует объект с конкретным ID.
     */
    public void register(Identifier id, int numericalId, T value) {
        if (idToIdentifier.containsKey(numericalId)) {
            Identifier existingId = idToIdentifier.get(numericalId);
            if (existingId.equals(id)) {
                // Тот же идентификатор, просто обновляем значение в базовом реестре
                super.register(id, value);
                return;
            }
            Logger.warn("Numerical ID collision! ID " + numericalId + " is already taken by " + existingId + ". " + id + " will be assigned a new ID.");
            register(id, value);
            return;
        }
        
        super.register(id, value);
        idToIdentifier.put(numericalId, id);
        identifierToId.put(id, numericalId);
        
        // Обновляем nextId если нужно, чтобы не предлагать занятые ID
        if (numericalId >= nextId) {
            nextId = numericalId + 1;
        }
    }

    /**
     * Находит следующий свободный числовой ID.
     */
    public int getNextAvailableId() {
        while (idToIdentifier.containsKey(nextId)) {
            nextId++;
        }
        return nextId;
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


