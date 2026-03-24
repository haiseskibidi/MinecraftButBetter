package com.za.minecraft.world.journal;

import com.za.minecraft.utils.Identifier;
import java.util.List;

public record JournalCategory(
    Identifier id,
    String name,
    Identifier icon,
    List<Identifier> entries
) {}
