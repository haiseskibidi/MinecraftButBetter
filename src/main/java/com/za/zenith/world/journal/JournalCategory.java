package com.za.zenith.world.journal;

import com.za.zenith.utils.Identifier;
import java.util.List;

public record JournalCategory(
    Identifier id,
    String name,
    Identifier icon,
    List<Identifier> entries
) {}
