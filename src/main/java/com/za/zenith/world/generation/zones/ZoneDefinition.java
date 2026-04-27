package com.za.zenith.world.generation.zones;

import com.google.gson.annotations.SerializedName;
import com.za.zenith.utils.Identifier;

import java.util.List;
import java.util.stream.Collectors;

public class ZoneDefinition {
    private transient Identifier id;
    
    @SerializedName("allowed_biomes")
    private List<String> allowedBiomes;

    public Identifier getId() { return id; }
    public void setId(Identifier id) { this.id = id; }
    
    public List<Identifier> getAllowedBiomes() {
        if (allowedBiomes == null) return List.of();
        return allowedBiomes.stream().map(Identifier::of).collect(Collectors.toList());
    }
}
