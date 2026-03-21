package com.za.minecraft.utils.events;

import java.util.ArrayList;
import java.util.List;

public class RegistryEvents {
    private static final List<Runnable> BLOCK_REGISTRATION_LISTENERS = new ArrayList<>();
    private static final List<Runnable> ITEM_REGISTRATION_LISTENERS = new ArrayList<>();

    public static void subscribeBlocks(Runnable listener) {
        BLOCK_REGISTRATION_LISTENERS.add(listener);
    }

    public static void subscribeItems(Runnable listener) {
        ITEM_REGISTRATION_LISTENERS.add(listener);
    }

    public static void fireBlockRegistration() {
        BLOCK_REGISTRATION_LISTENERS.forEach(Runnable::run);
    }

    public static void fireItemRegistration() {
        ITEM_REGISTRATION_LISTENERS.forEach(Runnable::run);
    }
}
