package com.za.zenith.engine.graphics.model;

import com.za.zenith.entities.parkour.animation.AnimationProfile;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;

public class ViewmodelController {

    private static class Snapshot {
        final Vector3f translation = new Vector3f();
        final Vector3f rotation = new Vector3f();
        final Quaternionf rotationQuat = new Quaternionf();
    }

    private final Map<String, Snapshot> snapshots = new HashMap<>();
    private float transitionTimer = 1.0f;
    private float transitionDuration = 0.25f;

    public void startTransition(Viewmodel viewmodel, float duration) {
        saveSnapshotRecursive(viewmodel.root);
        this.transitionTimer = 0.0f;
        this.transitionDuration = duration;
    }

    private void saveSnapshotRecursive(ModelNode node) {
        Snapshot snap = snapshots.computeIfAbsent(node.name, k -> new Snapshot());
        snap.translation.set(node.animTranslation);
        snap.rotation.set(node.animRotation);
        snap.rotationQuat.set(node.animRotationQuat);
        
        for (ModelNode child : node.children) {
            saveSnapshotRecursive(child);
        }
    }

    public void updateTransition(float deltaTime) {
        if (transitionTimer < 1.0f) {
            transitionTimer += deltaTime / transitionDuration;
            if (transitionTimer > 1.0f) transitionTimer = 1.0f;
        }
    }

    public void applyTransition(Viewmodel viewmodel) {
        if (transitionTimer >= 1.0f) return;
        applyTransitionRecursive(viewmodel.root);
    }

    private void applyTransitionRecursive(ModelNode node) {
        Snapshot snap = snapshots.get(node.name);
        if (snap != null) {
            // Lerp translations
            node.animTranslation.lerp(snap.translation, 1.0f - transitionTimer);
            // Lerp V1 rotations
            node.animRotation.lerp(snap.rotation, 1.0f - transitionTimer);
            // Slerp V2 quaternions
            Quaternionf temp = new Quaternionf(snap.rotationQuat);
            temp.slerp(node.animRotationQuat, transitionTimer);
            node.animRotationQuat.set(temp);
        }
        for (ModelNode child : node.children) {
            applyTransitionRecursive(child);
        }
    }

    public void applyAnimation(Viewmodel viewmodel, AnimationProfile profile, float time, float multiplier) {
        if (profile == null) return;
        
        for (ModelNode node : viewmodel.root.children) {
            applyRecursive(node, profile, time, multiplier);
        }
    }

    private void applyRecursive(ModelNode node, AnimationProfile profile, float time, float multiplier) {
        String name = node.name;
        
        if (profile.getVersion() == 1) {
            if (profile.hasTrack(name + ":x")) node.animTranslation.x = profile.evaluate(name + ":x", time, multiplier) / 16.0f;
            if (profile.hasTrack(name + ":y")) node.animTranslation.y = profile.evaluate(name + ":y", time, multiplier) / 16.0f;
            if (profile.hasTrack(name + ":z")) node.animTranslation.z = profile.evaluate(name + ":z", time, multiplier) / 16.0f;
            
            if (profile.hasTrack(name + ":pitch")) node.animRotation.x = (float)Math.toRadians(profile.evaluate(name + ":pitch", time, multiplier));
            if (profile.hasTrack(name + ":yaw")) node.animRotation.y = (float)Math.toRadians(profile.evaluate(name + ":yaw", time, multiplier));
            if (profile.hasTrack(name + ":roll")) node.animRotation.z = (float)Math.toRadians(profile.evaluate(name + ":roll", time, multiplier));
        } else {
            if (profile.hasTrack(name + ":x")) node.animTranslation.x += profile.evaluate(name + ":x", time, multiplier) / 16.0f;
            if (profile.hasTrack(name + ":y")) node.animTranslation.y += profile.evaluate(name + ":y", time, multiplier) / 16.0f;
            if (profile.hasTrack(name + ":z")) node.animTranslation.z += profile.evaluate(name + ":z", time, multiplier) / 16.0f;
            
            float pitch = profile.hasTrack(name + ":pitch") ? profile.evaluate(name + ":pitch", time, multiplier) : 0;
            float yaw = profile.hasTrack(name + ":yaw") ? profile.evaluate(name + ":yaw", time, multiplier) : 0;
            float roll = profile.hasTrack(name + ":roll") ? profile.evaluate(name + ":roll", time, multiplier) : 0;
            
            if (pitch != 0 || yaw != 0 || roll != 0) {
                Quaternionf q = new Quaternionf().rotationXYZ(
                    (float)Math.toRadians(pitch),
                    (float)Math.toRadians(yaw),
                    (float)Math.toRadians(roll)
                );
                
                // Additive blend via slerp to identity if multiplier is used as a weight.
                // But in V2, if multiplier is weight, we do a slerp with identity before multiplying
                if (multiplier < 1.0f && multiplier > 0.0f) {
                    Quaternionf identity = new Quaternionf();
                    identity.slerp(q, multiplier, q);
                } else if (multiplier < 0.0f) {
                    // For mirror track support
                    q.x *= -1;
                    q.y *= -1;
                    q.z *= -1;
                }
                
                node.animRotationQuat.mul(q);
            }
        }
        
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
        node.animRotationQuat.identity();
        for (ModelNode child : node.children) {
            resetRecursive(child);
        }
    }
}


