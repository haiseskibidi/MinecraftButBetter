package com.za.minecraft.world.items;

public class Item {
    protected final byte id;
    protected final String name;
    protected final String texturePath;

    public Item(byte id, String name, String texturePath) {
        this.id = id;
        this.name = name;
        this.texturePath = texturePath;
    }

    public byte getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTexturePath() {
        return texturePath;
    }
    
    public boolean isTool() {
        return false;
    }

    public boolean isFood() {
        return false;
    }

    public boolean isBlock() {
        return false;
    }

    public static class ViewmodelTransform {
        public float px, py, pz;
        public float rx, ry, rz;
        public float scale;

        public ViewmodelTransform(float px, float py, float pz, float rx, float ry, float rz, float scale) {
            this.px = px; this.py = py; this.pz = pz;
            this.rx = rx; this.ry = ry; this.rz = rz;
            this.scale = scale;
        }
    }

    private static final ViewmodelTransform DEFAULT_TRANSFORM = new ViewmodelTransform(0.55f, -0.65f, -0.75f, 0.0f, 90.0f, 0.0f, 0.85f);


    public ViewmodelTransform getViewmodelTransform() {
        return DEFAULT_TRANSFORM;
    }

    public float getMiningSpeed(byte blockType) {
        return 0.5f; // Base hand speed
    }
}
