package com.za.zenith.engine.resources.loaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.za.zenith.engine.resources.AbstractJsonLoader;
import com.za.zenith.utils.Logger;
import com.za.zenith.entities.parkour.animation.*;

public class AnimationDataLoader extends AbstractJsonLoader<AnimationProfile> {

    public AnimationDataLoader() {
        super("animations");
    }

    @Override
    protected void parseAndRegister(JsonElement el, String sourcePath) {
        try {
            String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
            String name = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
            
            JsonObject animObj = el.getAsJsonObject();
            AnimationProfile anim = new AnimationProfile(name);
            anim.setSourcePath(sourcePath);
            
            if (animObj.has("duration")) anim.setDuration(animObj.get("duration").getAsFloat());
            if (animObj.has("duration_key")) anim.setDurationKey(animObj.get("duration_key").getAsString());
            if (animObj.has("looping")) anim.setLooping(animObj.get("looping").getAsBoolean());
            if (animObj.has("version")) anim.setVersion(animObj.get("version").getAsInt());
            
            if (animObj.has("path")) {
                JsonObject p = animObj.getAsJsonObject("path");
                if (p.has("type")) anim.setPathType(p.get("type").getAsString());
                if (p.has("interpolation")) anim.setPathInterpolation(p.get("interpolation").getAsString());
                if (p.has("apex_y_offset")) anim.setApexYOffset(p.get("apex_y_offset").getAsFloat());
            }
            
            if (animObj.has("jitter")) {
                JsonObject j = animObj.getAsJsonObject("jitter");
                anim.setJitterEnabled(j.get("enabled").getAsBoolean());
                anim.setJitterStart(j.get("start").getAsFloat());
                anim.setJitterEnd(j.get("end").getAsFloat());
                anim.setJitterIntensity(j.get("intensity").getAsFloat());
            }
            
            if (animObj.has("tracks")) {
                anim.clearTracks(); // CRITICAL FOR HOT RELOAD
                JsonObject tracks = animObj.getAsJsonObject("tracks");
                for (String trackKey : tracks.keySet()) {
                    AnimationTrack track = new AnimationTrack();
                    JsonArray keys;
                    
                    if (tracks.get(trackKey).isJsonObject()) {
                        JsonObject tObj = tracks.getAsJsonObject(trackKey);
                        if (tObj.has("mirror")) track.setMirror(tObj.get("mirror").getAsBoolean());
                        keys = tObj.getAsJsonArray("keyframes");
                    } else {
                        keys = tracks.getAsJsonArray(trackKey);
                    }
                    
                    for (JsonElement ke : keys) {
                        if (ke.isJsonArray()) {
                            JsonArray ka = ke.getAsJsonArray();
                            track.addKeyframe(new Keyframe(
                                ka.get(0).getAsFloat(),
                                ka.get(1).getAsFloat(),
                                ka.get(2).getAsString()
                            ));
                        } else if (ke.isJsonObject()) {
                            track.addKeyframe(com.za.zenith.engine.resources.AssetManager.getGson().fromJson(ke, Keyframe.class));
                        }
                    }
                    anim.addTrack(trackKey, track);
                }
            }
            AnimationRegistry.register(name, anim);
        } catch (Exception e) {
            Logger.error("Failed to parse animation " + sourcePath + ": " + e.getMessage());
        }
    }
}