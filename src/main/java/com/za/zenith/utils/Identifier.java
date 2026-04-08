package com.za.zenith.utils;

import java.util.Objects;

/**
 * Уникальный идентификатор ресурса в формате namespace:path.
 * По умолчанию используется пространство имен "zenith".
 */
public class Identifier {
    public static final String DEFAULT_NAMESPACE = "zenith";
    
    private final String namespace;
    private final String path;

    public Identifier(String namespace, String path) {
        this.namespace = namespace.toLowerCase();
        this.path = path.toLowerCase();
    }

    public Identifier(String full) {
        if (full.contains(":")) {
            String[] parts = full.split(":", 2);
            this.namespace = parts[0].toLowerCase();
            this.path = parts[1].toLowerCase();
        } else {
            this.namespace = DEFAULT_NAMESPACE;
            this.path = full.toLowerCase();
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identifier that = (Identifier) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }
    
    public static Identifier of(String full) {
        return new Identifier(full);
    }
    
    public static Identifier of(String namespace, String path) {
        return new Identifier(namespace, path);
    }
}


