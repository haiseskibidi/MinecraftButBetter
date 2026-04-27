package com.za.zenith.world.generation.rules;

import com.google.gson.annotations.SerializedName;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.BlockRegistry;

public class SurfaceRule {
    @SerializedName("min_y")
    private Integer minY;

    @SerializedName("max_y")
    private Integer maxY;

    @SerializedName("min_depth")
    private Integer minDepth;

    @SerializedName("max_depth")
    private Integer maxDepth;

    @SerializedName("min_noise")
    private Float minNoise;

    @SerializedName("max_noise")
    private Float maxNoise;

    @SerializedName("block")
    private String blockId;

    private transient Identifier cachedBlockId;

    public boolean evaluate(int x, int y, int z, float noiseVal, int depth) {
        if (minY != null && y < minY) return false;
        if (maxY != null && y > maxY) return false;
        if (minDepth != null && depth < minDepth) return false;
        if (maxDepth != null && depth > maxDepth) return false;
        if (minNoise != null && noiseVal < minNoise) return false;
        if (maxNoise != null && noiseVal > maxNoise) return false;
        return true;
    }

    public BlockDefinition getBlock() {
        if (cachedBlockId == null && blockId != null) {
            cachedBlockId = Identifier.of(blockId);
        }
        if (cachedBlockId != null) {
            return BlockRegistry.getBlock(cachedBlockId);
        }
        return null;
    }
}
