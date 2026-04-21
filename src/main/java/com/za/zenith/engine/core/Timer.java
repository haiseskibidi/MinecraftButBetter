package com.za.zenith.engine.core;

public class Timer {
    private long lastTime;
    private double delta;
    private long frames = 0;
    
    private final long startTime;

    public Timer() {
        startTime = System.nanoTime();
        lastTime = startTime;
    }
    
    public double getTotalTime() {
        return (System.nanoTime() - startTime) / 1_000_000_000.0;
    }

    public void updateDelta() {
        long currentTime = System.nanoTime();
        delta = (currentTime - lastTime) / 1_000_000_000.0;
        lastTime = currentTime;
        frames++;
    }
    
    public long getFrames() {
        return frames;
    }
    
    public double getDelta() {
        return delta;
    }
    
    public float getDeltaF() {
        return (float) delta;
    }
}


