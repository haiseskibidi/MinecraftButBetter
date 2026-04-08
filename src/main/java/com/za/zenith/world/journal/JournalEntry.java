package com.za.zenith.world.journal;

import com.za.zenith.utils.Identifier;
import java.util.List;

public record JournalEntry(
    Identifier id,
    String title,
    Identifier icon,
    List<JournalElement> elements
) {}
