package com.za.zenith.engine.graphics.ui;

import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.utils.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class FontRenderer {
    private static final int GRID_SIZE = 16;
    private static final int GLYPH_SIZE = 8;

    private final int[] widthMap = new int[256];
    private final int[] unicodeWidthMap = new int[65536];
    private final java.util.Map<Integer, Integer> unicodePages = new java.util.HashMap<>();
    private final java.util.Map<Integer, GlyphInfo> glyphCustomMap = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> textureCache = new java.util.HashMap<>();

    private static class GlyphInfo {
        int textureId;
        int gridX, gridY;
        int totalCols, totalRows;
        int width;

        GlyphInfo(int textureId, int gridX, int gridY, int totalCols, int totalRows, int width) {
            this.textureId = textureId;
            this.gridX = gridX;
            this.gridY = gridY;
            this.totalCols = totalCols;
            this.totalRows = totalRows;
            this.width = width;
        }
    }

    private Shader shader;
    private int vao;
    private int vbo;
    private int ebo;

    private final float[][] colorCodes = new float[32][3];

    public void init(Shader shader) {
        this.shader = shader;
        setupColorCodes();
        // Загружаем ширину для ASCII (fallback)
        loadWidthMap("zenith/textures/font/ascii.png", widthMap);
        
        // Загружаем все маппинги из JSON
        loadJsonMapping();
        
        createQuad();
    }

    private void setupColorCodes() {
        for (int i = 0; i < 32; ++i) {
            int j = (i >> 3 & 1) * 85;
            int k = (i >> 2 & 1) * 170 + j;
            int l = (i >> 1 & 1) * 170 + j;
            int m = (i & 1) * 170 + j;
            if (i == 6) k += 85;
            if (i >= 16) { k /= 4; l /= 4; m /= 4; }
            colorCodes[i][0] = (float)k / 255.0F;
            colorCodes[i][1] = (float)l / 255.0F;
            colorCodes[i][2] = (float)m / 255.0F;
        }
    }

    private void loadJsonMapping() {
        try (InputStream stream = FontRenderer.class.getClassLoader().getResourceAsStream("zenith/font/default.json")) {
            if (stream == null) return;
            String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            com.google.gson.JsonArray providers = root.getAsJsonArray("providers");
            
            for (com.google.gson.JsonElement element : providers) {
                com.google.gson.JsonObject provider = element.getAsJsonObject();
                if ("bitmap".equals(provider.get("type").getAsString())) {
                    String file = provider.get("file").getAsString();
                    String resourcePath = file.replace("zenith:", "zenith/textures/");
                    
                    BufferedImage image = loadImage(resourcePath);
                    if (image == null) continue;
                    
                    int textureId = getOrLoadTexture(resourcePath);
                    com.google.gson.JsonArray charsArr = provider.getAsJsonArray("chars");
                    
                    int imgW = image.getWidth();
                    int imgH = image.getHeight();
                    int rows = charsArr.size();
                    int cols = 0;
                    
                    // Считаем колонки по кодовым точкам, а не по длине строки char[]
                    for (com.google.gson.JsonElement lineElem : charsArr) {
                        String line = lineElem.getAsString();
                        cols = Math.max(cols, (int) line.codePoints().count());
                    }
                    
                    if (cols == 0) continue;
                    int charW = imgW / cols;
                    int charH = imgH / rows;
                    
                    int[] pixels = new int[imgW * imgH];
                    image.getRGB(0, 0, imgW, imgH, pixels, 0, imgW);

                    for (int row = 0; row < rows; row++) {
                        String line = charsArr.get(row).getAsString();
                        int[] codePoints = line.codePoints().toArray();
                        for (int col = 0; col < codePoints.length; col++) {
                            int cp = codePoints[col];
                            if (cp == 0 || cp == '\u0000') continue;
                            
                            int width = calculateCharWidth(pixels, imgW, col, row, charW, charH);
                            glyphCustomMap.put(cp, new GlyphInfo(textureId, col, row, cols, rows, width));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to load font mapping: " + e.getMessage());
        }
    }

    private int getOrLoadTexture(String path) {
        if (textureCache.containsKey(path)) {
            return textureCache.get(path);
        }
        int id = loadTexture(path);
        textureCache.put(path, id);
        return id;
    }

    private BufferedImage loadImage(String path) {
        try (InputStream stream = FontRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            return ImageIO.read(stream);
        } catch (IOException e) {
            return null;
        }
    }

    private int calculateCharWidth(int[] pixels, int imgW, int gridX, int gridY, int charW, int charH) {
        int maxColumn = charW - 1;
        boolean emptyColumn;
        do {
            emptyColumn = true;
            for (int py = 0; py < charH; py++) {
                int px = (gridX * charW + maxColumn);
                int py_idx = (gridY * charH + py);
                int pixelIndex = py_idx * imgW + px;
                if (pixelIndex >= pixels.length) break;
                int alpha = (pixels[pixelIndex] >> 24) & 0xFF;
                if (alpha > 0x20) {
                    emptyColumn = false;
                    break;
                }
            }
            if (emptyColumn && maxColumn >= 0) {
                maxColumn--;
            }
        } while (emptyColumn && maxColumn >= 0);
        return maxColumn + 2;
    }

    public void drawString(String text, int x, int y, int size, int screenWidth, int screenHeight) {
        drawString(text, x, y, size, screenWidth, screenHeight, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void drawString(String text, int x, int y, int size, int screenWidth, int screenHeight, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) {
            return;
        }

        float scale = (float) size / GLYPH_SIZE;

        shader.use();
        shader.setInt("useTexture", 1);
        shader.setInt("useArray", 0);
        
        float currentR = r, currentG = g, currentB = b;
        boolean bold = false;

        glActiveTexture(GL_TEXTURE0);
        glBindVertexArray(vao);

        int drawX = x;
        int lastBoundTexture = -1;

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);

            if (cp == '$' && i + charCount < text.length()) {
                int nextCp = text.codePointAt(i + charCount);
                int codeIndex = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(nextCp));

                if (codeIndex >= 0 && codeIndex < 16) {
                    currentR = colorCodes[codeIndex][0];
                    currentG = colorCodes[codeIndex][1];
                    currentB = colorCodes[codeIndex][2];
                } else if (codeIndex == 16) { // k - magic (skip for now)
                } else if (codeIndex == 17) { // l - bold
                    bold = true;
                } else if (codeIndex == 21) { // r - reset
                    currentR = r; currentG = g; currentB = b;
                    bold = false;
                }

                i += charCount + Character.charCount(nextCp);
                continue;
            }

            shader.setUniform("tintColor", currentR, currentG, currentB, a);

            int textureToBind;
            GlyphInfo custom = glyphCustomMap.get(cp);
            
            if (custom != null) {
                textureToBind = custom.textureId;
            } else {
                int page = cp / 256;
                if (page == 0 && cp < 128) {
                    textureToBind = getOrLoadTexture("zenith/textures/font/ascii.png");
                } else {
                    textureToBind = getUnicodePageTexture(page);
                }
            }
            
            if (textureToBind != lastBoundTexture) {
                glBindTexture(GL_TEXTURE_2D, textureToBind);
                lastBoundTexture = textureToBind;
            }

            int glyphWidth;
            if (custom != null) {
                glyphWidth = custom.width;
            } else {
                glyphWidth = getGlyphWidth((char)cp);
            }
            
            float advance = glyphWidth * scale;

            if (custom != null) {
                renderCustomGlyph(custom, drawX, y, size, screenWidth, screenHeight);
                if (bold) renderCustomGlyph(custom, drawX + Math.round(0.5f * scale), y, size, screenWidth, screenHeight);
                drawX += Math.round(advance + scale);
            } else {
                int page = cp / 256;
                if (page != 0 || cp >= 128) {
                    renderGlyph((char)cp, drawX, y, size, screenWidth, screenHeight);
                    if (bold) renderGlyph((char)cp, drawX + Math.round(0.5f * scale), y, size, screenWidth, screenHeight);
                    drawX += Math.round(advance); 
                } else {
                    renderGlyph((char)cp, drawX, y, size, screenWidth, screenHeight);
                    if (bold) renderGlyph((char)cp, drawX + Math.round(0.5f * scale), y, size, screenWidth, screenHeight);
                    drawX += Math.round(advance + scale);
                }
            }
            i += charCount;
        }

        glBindVertexArray(0);
    }

    public void drawWrappedString(String text, int x, int y, int size, int maxWidth, int screenWidth, int screenHeight) {
        drawWrappedString(text, x, y, size, maxWidth, screenWidth, screenHeight, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void drawWrappedString(String text, int x, int y, int size, int maxWidth, int screenWidth, int screenHeight, float r, float g, float b, float a) {
        if (text == null || text.isEmpty()) return;
        
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int currentY = y;
        int lineHeight = (int)(size * 1.3f); // Увеличил интервал для читаемости
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine.toString() + " " + word;
            if (getStringWidth(testLine, size) > maxWidth) {
                if (currentLine.length() > 0) {
                    drawString(currentLine.toString(), x, currentY, size, screenWidth, screenHeight, r, g, b, a);
                    currentY += lineHeight;
                    currentLine = new StringBuilder(word);
                } else {
                    drawString(word, x, currentY, size, screenWidth, screenHeight, r, g, b, a);
                    currentY += lineHeight;
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        
        if (currentLine.length() > 0) {
            drawString(currentLine.toString(), x, currentY, size, screenWidth, screenHeight, r, g, b, a);
        }
    }

    public int getWrappedStringHeight(String text, int size, int maxWidth) {
        if (text == null || text.isEmpty()) return 0;
        
        String[] words = text.split(" ");
        int lines = 1;
        StringBuilder currentLine = new StringBuilder();
        int lineHeight = (int)(size * 1.3f);
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine.toString() + " " + word;
            if (getStringWidth(testLine, size) > maxWidth) {
                if (currentLine.length() > 0) {
                    lines++;
                    currentLine = new StringBuilder(word);
                } else {
                    lines++;
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        
        return lines * lineHeight;
    }

    private void renderCustomGlyph(GlyphInfo glyph, int x, int y, int size, int screenWidth, int screenHeight) {
        float uvStepX = 1.0f / glyph.totalCols;
        float uvStepY = 1.0f / glyph.totalRows;
        float uvX = glyph.gridX * uvStepX;
        float uvY = glyph.gridY * uvStepY;

        float screenX = (2.0f * (x + size / 2.0f) / screenWidth) - 1.0f;
        float screenY = 1.0f - (2.0f * (y + size / 2.0f) / screenHeight);

        shader.setUniform("scale", (float) size / screenWidth, (float) size / screenHeight, 0.0f, 0.0f);
        shader.setUniform("position_offset", screenX, screenY, 0.0f, 0.0f);
        shader.setUniform("uvOffset", uvX, uvY, 0.0f, 0.0f);
        shader.setUniform("uvScale", uvStepX, uvStepY, 0.0f, 0.0f);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    public int getStringWidth(String text, int size) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float scale = (float) size / GLYPH_SIZE;
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);

            if (cp == '$' && i + charCount < text.length()) {
                int nextCp = text.codePointAt(i + charCount);
                i += charCount + Character.charCount(nextCp);
                continue;
            }
            GlyphInfo custom = glyphCustomMap.get(cp);
            if (custom != null) {
                width += Math.round(custom.width * scale + scale);
            } else {
                int page = cp / 256;
                if (page != 0 || cp >= 128) {
                    width += Math.round(getGlyphWidth((char)cp) * scale);
                } else {
                    width += Math.round(getGlyphWidth((char)cp) * scale + scale);
                }
            }
            i += charCount;
        }
        return width;
    }

    private int getUnicodePageTexture(int page) {
        if (unicodePages.containsKey(page)) {
            return unicodePages.get(page);
        }
        
        String pageHex = String.format("%02x", page);
        String path = "zenith/textures/font/unicode_page_" + pageHex + ".png";
        loadUnicodeWidthMap(path, page * 256);
        
        int textureId = loadTexture(path);
        unicodePages.put(page, textureId);
        return textureId;
    }

    private int loadTexture(String path) {
        try (InputStream stream = FontRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return 0;
            BufferedImage image = ImageIO.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();

            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
            for (int pixel : pixels) {
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
            buffer.flip();

            int texId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            MemoryUtil.memFree(buffer);
            return texId;
        } catch (IOException e) {
            return 0;
        }
    }

    private void renderGlyph(char c, int x, int y, int size, int screenWidth, int screenHeight) {
        int glyphIndex = c % 256;
        int gridX = glyphIndex % GRID_SIZE;
        int gridY = glyphIndex / GRID_SIZE;

        float uvX = gridX / (float) GRID_SIZE;
        float uvY = gridY / (float) GRID_SIZE;
        float uvSize = 1.0f / GRID_SIZE;

        float screenX = (2.0f * (x + size / 2.0f) / screenWidth) - 1.0f;
        float screenY = 1.0f - (2.0f * (y + size / 2.0f) / screenHeight);

        shader.setUniform("scale", (float) size / screenWidth, (float) size / screenHeight, 0.0f, 0.0f);
        shader.setUniform("position_offset", screenX, screenY, 0.0f, 0.0f);
        shader.setUniform("uvOffset", uvX, uvY, 0.0f, 0.0f);
        shader.setUniform("uvScale", uvSize, uvSize, 0.0f, 0.0f);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    private void loadWidthMap(String path, int[] targetMap) {
        try (InputStream stream = FontRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return;
            BufferedImage image = ImageIO.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            int charWidth = width / 16;
            int charHeight = height / 16;

            for (int i = 0; i < 256; i++) {
                int col = i % 16;
                int row = i / 16;
                int maxColumn = charWidth - 1;
                boolean emptyColumn;
                do {
                    emptyColumn = true;
                    for (int py = 0; py < charHeight; py++) {
                        int pixelIndex = (row * charHeight + py) * width + (col * charWidth + maxColumn);
                        int alpha = (pixels[pixelIndex] >> 24) & 0xFF;
                        if (alpha > 0x80) {
                            emptyColumn = false;
                            break;
                        }
                    }
                    if (emptyColumn && maxColumn >= 0) {
                        maxColumn--;
                    }
                } while (emptyColumn && maxColumn >= 0);

                if (i == 32) targetMap[i] = 4;
                else targetMap[i] = maxColumn + 2;
            }
        } catch (IOException e) {
            Logger.error("Failed to load width map: " + path);
        }
    }

    private void loadUnicodeWidthMap(String path, int startChar) {
        try (InputStream stream = FontRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return;
            BufferedImage image = ImageIO.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            int charWidth = width / 16;
            int charHeight = height / 16;

            for (int i = 0; i < 256; i++) {
                int col = i % 16;
                int row = i / 16;
                int maxColumn = charWidth - 1;
                boolean emptyColumn;
                do {
                    emptyColumn = true;
                    for (int py = 0; py < charHeight; py++) {
                        int pixelIndex = (row * charHeight + py) * width + (col * charWidth + maxColumn);
                        int alpha = (pixels[pixelIndex] >> 24) & 0xFF;
                        if (alpha > 0x20) {
                            emptyColumn = false;
                            break;
                        }
                    }
                    if (emptyColumn && maxColumn >= 0) {
                        maxColumn--;
                    }
                } while (emptyColumn && maxColumn >= 0);

                if (maxColumn < 0) {
                    unicodeWidthMap[startChar + i] = 0;
                } else {
                    unicodeWidthMap[startChar + i] = maxColumn + 2;
                }
            }
        } catch (IOException e) {
            Logger.error("Failed to load unicode width map: " + path);
        }
    }

    private int getGlyphWidth(char c) {
        if (c < 256) return widthMap[c];
        int w = unicodeWidthMap[c];
        return (w > 0) ? w : 6;
    }

    private void createQuad() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        float[] vertices = {
            -1.0f, -1.0f, 0.0f, 1.0f, // LB
             1.0f, -1.0f, 1.0f, 1.0f, // RB
             1.0f,  1.0f, 1.0f, 0.0f, // RT
            -1.0f,  1.0f, 0.0f, 0.0f  // LT
        };

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        int[] indices = {0, 1, 2, 2, 3, 0};
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }

    public void cleanup() {
        for (int texId : textureCache.values()) {
            glDeleteTextures(texId);
        }
        for (int texId : unicodePages.values()) {
            glDeleteTextures(texId);
        }
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}


