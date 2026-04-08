package com.za.zenith.engine.graphics.model;

import com.za.zenith.entities.parkour.animation.AnimationProfile;

public class ViewmodelController {
    public void applyAnimation(Viewmodel viewmodel, AnimationProfile profile, float time, float multiplier) {
        if (profile == null) return;
        
        for (ModelNode node : viewmodel.root.children) {
            applyRecursive(node, profile, time, multiplier);
        }
    }

    private void applyRecursive(ModelNode node, AnimationProfile profile, float time, float multiplier) {
        String name = node.name;
        
        // Evaluate bone-specific tracks only if they exist
        // Positions are added to base offsets
        if (profile.hasTrack(name + ":x")) node.animTranslation.x = profile.evaluate(name + ":x", time, multiplier) / 16.0f;
        if (profile.hasTrack(name + ":y")) node.animTranslation.y = profile.evaluate(name + ":y", time, multiplier) / 16.0f;
        if (profile.hasTrack(name + ":z")) node.animTranslation.z = profile.evaluate(name + ":z", time, multiplier) / 16.0f;
        
        // Rotations are in degrees in JSON, convert to radians
        if (profile.hasTrack(name + ":pitch")) node.animRotation.x = (float)Math.toRadians(profile.evaluate(name + ":pitch", time, multiplier));
        if (profile.hasTrack(name + ":yaw")) node.animRotation.y = (float)Math.toRadians(profile.evaluate(name + ":yaw", time, multiplier));
        if (profile.hasTrack(name + ":roll")) node.animRotation.z = (float)Math.toRadians(profile.evaluate(name + ":roll", time, multiplier));
        
        for (ModelNode child : node.children) {
            applyRecursive(child, profile, time, multiplier);
        }
    }
    
    public void resetAnimation(Viewmodel viewmodel) {
        resetRecursive(viewmodel.root);
    }
    
    private void resetRecursive(ModelNode node) {
        node.animTranslation.set(0, 0, 0);
        node.animRotation.set(0, 0, 0);
        for (ModelNode child : node.children) {
            resetRecursive(child);
        }
    }
}


