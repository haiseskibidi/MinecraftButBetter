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
                key.add("smootherstep"); 
                keyList.add(key);
            }
            jsonTracks.put(prefix + suffix, keyList);
        }
    }
}
