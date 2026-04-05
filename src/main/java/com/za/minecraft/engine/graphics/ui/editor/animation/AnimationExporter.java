package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.za.minecraft.utils.Logger;
import org.joml.Vector3f;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Handles exporting AnimationEditorState to JSON files.
 */
public class AnimationExporter {
    
    public static void export(AnimationEditorState state, String path) {
        Map<String, Object> root = new HashMap<>();
        root.put("looping", false);
        root.put("duration", 1.0);
        
        Map<String, List<List<Object>>> jsonTracks = new LinkedHashMap<>();
        
        for (Map.Entry<String, AnimationEditorState.EditorTrack> entry : state.tracks.entrySet()) {
            String boneName = entry.getKey();
            AnimationEditorState.EditorTrack track = entry.getValue();
            if (track.keyframes.isEmpty()) continue;

            if (boneName.equals("Camera")) {
                addCameraTracks(jsonTracks, track);
                continue;
            }

            // Filtering logic: ignore tracks where all keyframes are zero
            boolean allZero = true;
            for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                if (k.pos().lengthSquared() > 0.0001f || k.rot().lengthSquared() > 0.0001f) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) continue;

            String prefix = boneName.contains("attachment") ? "item_" : boneName + ":";
            addTracksForBone(jsonTracks, prefix, track);
        }
        
        root.put("tracks", jsonTracks);
        
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Logger.info("Animation successfully exported to: " + path);
        } catch (IOException e) {
            Logger.error("Failed to export animation: " + e.getMessage());
        }
    }

    private static void addCameraTracks(Map<String, List<List<Object>>> jsonTracks, AnimationEditorState.EditorTrack track) {
        String[] mappedNames = {"camera_x", "camera_y", "fov_offset", "camera_tilt", "camera_roll"};
        for (String name : mappedNames) {
            boolean hasData = false;
            for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                float val = 0;
                switch (name) {
                    case "camera_x" -> val = k.pos().x;
                    case "camera_y" -> val = k.pos().y;
                    case "fov_offset" -> val = k.pos().z;
                    case "camera_tilt" -> val = k.rot().x;
                    case "camera_roll" -> val = k.rot().z;
                }
                if (Math.abs(val) > 0.0001f) { hasData = true; break; }
            }
            if (!hasData) continue;

            String easingStr = "linear";
            switch (track.easing) {
                case SINE_IN_OUT -> easingStr = "smooth";
                case QUAD_IN_OUT, CUBIC_IN_OUT -> easingStr = "smootherstep";
            }

            List<List<Object>> keyList = new ArrayList<>();
            for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                float val = 0;
                switch (name) {
                    case "camera_x" -> val = k.pos().x * 16.0f;
                    case "camera_y" -> val = k.pos().y * 16.0f;
                    case "fov_offset" -> val = k.pos().z * 16.0f;
                    case "camera_tilt" -> val = (float)Math.toDegrees(k.rot().x);
                    case "camera_roll" -> val = (float)Math.toDegrees(k.rot().z);
                }
                List<Object> key = new ArrayList<>();
                key.add(k.time());
                key.add(val);
                key.add(easingStr);
                keyList.add(key);
            }
            jsonTracks.put(name, keyList);
        }
    }

    private static void addTracksForBone(Map<String, List<List<Object>>> jsonTracks, String prefix, AnimationEditorState.EditorTrack track) {
        String[] suffixes = {"x", "y", "z", "pitch", "yaw", "roll"};
        for (String suffix : suffixes) {
            // Check if this specific axis/property has any non-zero keys
            boolean hasData = false;
            for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                float val = 0;
                switch (suffix) {
                    case "x" -> val = k.pos().x;
                    case "y" -> val = k.pos().y;
                    case "z" -> val = k.pos().z;
                    case "pitch" -> val = k.rot().x;
                    case "yaw" -> val = k.rot().y;
                    case "roll" -> val = k.rot().z;
                }
                if (Math.abs(val) > 0.0001f) {
                    hasData = true;
                    break;
                }
            }
            if (!hasData) continue;

            String easingStr = "linear";
            switch (track.easing) {
                case SINE_IN_OUT -> easingStr = "smooth";
                case QUAD_IN_OUT, CUBIC_IN_OUT -> easingStr = "smootherstep";
            }

            List<List<Object>> keyList = new ArrayList<>();
            for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                float val = 0;
                switch (suffix) {
                    case "x" -> val = k.pos().x * 16.0f;
                    case "y" -> val = k.pos().y * 16.0f;
                    case "z" -> val = k.pos().z * 16.0f;
                    case "pitch" -> val = (float)Math.toDegrees(k.rot().x);
                    case "yaw" -> val = (float)Math.toDegrees(k.rot().y);
                    case "roll" -> val = (float)Math.toDegrees(k.rot().z);
                }
                
                List<Object> key = new ArrayList<>();
                key.add(k.time());
                key.add(val);
                key.add(easingStr); 
                keyList.add(key);
            }
            jsonTracks.put(prefix + suffix, keyList);
        }
    }
}
