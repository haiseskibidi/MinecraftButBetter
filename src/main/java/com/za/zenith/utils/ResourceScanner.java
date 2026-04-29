package com.za.zenith.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class ResourceScanner {

    public static List<String> listResources(String path) {
        List<String> files = new ArrayList<>();
        
        // 1. Пытаемся найти .index файл для обратной совместимости или если сканирование не сработает
        String indexPath = path + "/.index";
        URL indexUrl = ResourceScanner.class.getClassLoader().getResource(indexPath);
        
        // 2. Основное сканирование
        try {
            URL url = ResourceScanner.class.getClassLoader().getResource(path);
            if (url != null) {
                URI uri = url.toURI();
                
                if (uri.getScheme().equals("jar")) {
                    // Сканирование внутри JAR
                    try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                        Path myPath = fileSystem.getPath(path);
                        files.addAll(listFromPath(myPath));
                    }
                } else {
                    // Сканирование в файловой системе (IDE)
                    Path myPath = Paths.get(uri);
                    files.addAll(listFromPath(myPath));
                }
            }
        } catch (Exception e) {
            Logger.warn("Failed to scan resources at " + path + ": " + e.getMessage());
        }

        // 3. Если ничего не нашли через сканирование, пробуем .index (фоллбэк)
        if (files.isEmpty() && indexUrl != null) {
            Logger.info("Falling back to .index for " + path);
            return listFromIndex(path);
        }

        return files;
    }

    private static List<String> listFromPath(Path path) throws IOException {
        List<String> files = new ArrayList<>();
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.list(path)) {
                Iterator<Path> it = walk.iterator();
                while (it.hasNext()) {
                    Path p = it.next();
                    String name = p.getFileName().toString();
                    if (!name.startsWith(".") && !Files.isDirectory(p)) {
                        files.add(name);
                    }
                }
            }
        }
        return files;
    }

    private static List<String> listFromIndex(String path) {
        List<String> files = new ArrayList<>();
        String indexPath = path + "/.index";
        try (java.io.InputStream is = ResourceScanner.class.getClassLoader().getResourceAsStream(indexPath)) {
            if (is != null) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        files.add(line.trim());
                    }
                }
            }
        } catch (Exception e) {
            Logger.warn("Could not read index for fallback " + path);
        }
        return files;
    }
}
