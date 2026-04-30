package com.za.zenith.engine.resources;

public interface IResourceLoader {
    void load(String namespace);
    
    /**
     * Перезагружает конкретный файл ресурса. 
     * @return true, если загрузчик обработал этот путь.
     */
    default boolean reload(String path) { return false; }
}
