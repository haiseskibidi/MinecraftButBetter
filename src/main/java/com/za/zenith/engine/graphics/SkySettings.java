package com.za.zenith.engine.graphics;

public class SkySettings {
    private static SkySettings instance = new SkySettings();

    public BodyConfig sun = new BodyConfig("procedural", null, 15.0f, new float[]{1.0f, 1.0f, 0.8f, 1.0f});
    public BodyConfig moon = new BodyConfig("procedural", null, 10.0f, new float[]{0.7f, 0.7f, 1.0f, 1.0f});

    public static class BodyConfig {
        public String type = "texture"; // texture, pixels, procedural
        public String texture;
        public float scale;
        public float[] color; // RGBA
        
        // For 'pixels' type
        public int width;
        public int height;
        public int[] pixels;

        public BodyConfig() {}

        public BodyConfig(String type, String texture, float scale, float[] color) {
            this.type = type;
            this.texture = texture;
            this.scale = scale;
            this.color = color;
        }
    }

    public static SkySettings getInstance() {
        return instance;
    }

    public static void setInstance(SkySettings newInstance) {
        instance = newInstance;
    }
}
