package com.za.zenith.world.items.loot;

import com.za.zenith.utils.Identifier;
import java.util.List;

/**
 * Data-driven loot table structure.
 */
public class LootTable implements com.za.zenith.utils.LiveReloadable {
    private final Identifier identifier;
    private final List<Pool> pools;
    private String sourcePath;

    public LootTable(Identifier identifier, List<Pool> pools) {
        this.identifier = identifier;
        this.pools = pools;
    }

    public Identifier identifier() { return identifier; }
    public List<Pool> pools() { return pools; }

    @Override
    public String getSourcePath() { return sourcePath; }

    @Override
    public void setSourcePath(String path) { this.sourcePath = path; }

    public static record Pool(
        int rolls,
        List<Entry> entries
    ) {}

    public static record Entry(
        Identifier item,
        int weight
    ) {}
}
