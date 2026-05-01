package com.za.zenith.entities;

import com.za.zenith.utils.Identifier;
import org.joml.Vector3f;

/**
 * Определение типа сущности из JSON.
 * Хранит визуальные и физические параметры.
 */
public class EntityDefinition implements com.za.zenith.utils.LiveReloadable {
    private final Identifier identifier;
    private final String modelType; // "item", "block", "custom"
    private final String texture;   // Путь к текстуре или ID блока
    private final Vector3f visualScale;
    private final Vector3f hitboxSize;
    private String sourcePath;

    public EntityDefinition(Identifier identifier, String modelType, String texture, Vector3f visualScale, Vector3f hitboxSize) {
        this.identifier = identifier;
        this.modelType = modelType;
        this.texture = texture;
        this.visualScale = visualScale;
        this.hitboxSize = hitboxSize;
    }

    public Identifier identifier() { return identifier; }
    public String modelType() { return modelType; }
    public String texture() { return texture; }
    public Vector3f visualScale() { return visualScale; }
    public Vector3f hitboxSize() { return hitboxSize; }

    @Override
    public String getSourcePath() { return sourcePath; }

    @Override
    public void setSourcePath(String path) { this.sourcePath = path; }
}
