package com.za.zenith.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class I18n {
    private static Map<String, String> translations = new HashMap<>();
    private static String currentLanguage = "ru_ru";

    static {
        loadLanguage("en_us"); // Load English as fallback
        loadLanguage("ru_ru"); // Override with Russian
    }

    public static void loadLanguage(String langCode) {
        String path = "zenith/lang/" + langCode + ".json";
        try (InputStream is = I18n.class.getClassLoader().getResourceAsStream(path)) {
            if (is != null) {
                Gson gson = new Gson();
                Map<String, String> newTranslations = gson.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>(){}.getType()
                );
                if (newTranslations != null) {
                    translations.putAll(newTranslations);
                    currentLanguage = langCode;
                    Logger.info("Loaded language: " + langCode);
                }
            } else {
                Logger.error("Language file not found: " + path);
            }
        } catch (Exception e) {
            Logger.error("Failed to load language " + langCode + ": " + e.getMessage());
        }
    }

    public static String get(String key) {
        return translations.getOrDefault(key, key);
    }

    public static String format(String key, Object... args) {
        String pattern = get(key);
        try {
            return String.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }
}


