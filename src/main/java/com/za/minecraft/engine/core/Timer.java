package com.za.minecraft.engine.core;

public class Timer {
    private long lastTime;
    private double delta;
    
    public Timer() {
        lastTime = System.nanoTime();
    }
    
    public void updateDelta() {
        long currentTime = System.nanoTime();
        delta = (currentTime - lastTime) / 1_000_000_000.0;
        lastTime = currentTime;
    }
    
    public double getDelta() {
        return delta;
    }
    
    public float getDeltaF() {
        return (float) delta;
    }
}
