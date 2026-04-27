package com.za.zenith.world.generation;

import com.google.gson.annotations.SerializedName;
import com.za.zenith.utils.Identifier;

import java.util.ArrayList;
import java.util.List;

public class BiomeDefinition {
    private transient Identifier id;
    
    @SerializedName("climate_points")
    private List<ClimatePoint> climatePoints = new ArrayList<>();
    
    private float temperature; // Legacy support
    private float humidity;    // Legacy support
    
    @SerializedName("base_height")
    private int baseHeight = 64; 

    public static class ClimatePoint {
        public float temperature;
        public float humidity;
        public float continentalness;
        public float erosion;
        public float weirdness;
        public float offset = 0.0f; // Дополнительное смещение для приоритета
    }

    public List<ClimatePoint> getClimatePoints() {
        if (climatePoints.isEmpty()) {
            // Создаем точку по умолчанию из легаси полей
            ClimatePoint p = new ClimatePoint();
            p.temperature = temperature;
            p.humidity = humidity;
            climatePoints.add(p);
        }
        return climatePoints;
    }
    
    @SerializedName("height_variation")
    private int heightVariation = 15;
    
    @SerializedName("surface_block")
    private String surfaceBlock = "zenith:grass_block";
    
    @SerializedName("underground_block")
    private String undergroundBlock = "zenith:dirt";
    
    @SerializedName("tree_density")
    private double treeDensity = 0.0;
    
    @SerializedName("erosion_factor")
    private double erosionFactor = 1.0;
    
    private List<FeatureEntry> features = new ArrayList<>();

    public Identifier getId() { return id; }
    public void setId(Identifier id) { this.id = id; }
    
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    
    public int getBaseHeight() { return baseHeight; }
    public int getHeightVariation() { return heightVariation; }
    
    public Identifier getSurfaceBlock() { return Identifier.of(surfaceBlock); }
    public Identifier getUndergroundBlock() { return Identifier.of(undergroundBlock); }
    
    public double getTreeDensity() { return treeDensity; }
    public double getErosionFactor() { return erosionFactor; }
    public List<FeatureEntry> getFeatures() { return features; }

    public static class FeatureEntry {
        private String id;
        private int weight;
        
        public Identifier getId() { return Identifier.of(id); }
        public int getWeight() { return weight; }
    }
}
