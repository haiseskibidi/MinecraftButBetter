package com.za.minecraft.utils;

public class Logger {
    private static final boolean DEBUG_MODE = true;
    
    public static void debug(String message, Object... args) {
        if (DEBUG_MODE) {
            System.out.printf("[DEBUG] " + message + "%n", args);
        }
    }
    
    public static void info(String message, Object... args) {
        System.out.printf("[INFO] " + message + "%n", args);
    }
    
    public static void warn(String message, Object... args) {
        System.out.printf("[WARN] " + message + "%n", args);
    }
    
    public static void error(String message, Object... args) {
        System.err.printf("[ERROR] " + message + "%n", args);
    }
    
    public static void error(String message, Throwable t, Object... args) {
        error(message, args);
        t.printStackTrace();
    }
}
