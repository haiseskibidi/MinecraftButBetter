package com.za.minecraft.world.journal;

import com.za.minecraft.utils.Identifier;
import java.util.List;

public record JournalEntry(
    Identifier id,
    String title,
    Identifier icon,
    List<JournalElement> elements
) {}
