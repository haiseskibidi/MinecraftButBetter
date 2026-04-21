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
        loadWidthMap("zenith/textures/font/ascii.png", widthMap);
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
        drawString(text, x, y, size, screenWidth, screenHeight, r, g, b, a, false);
    }

    public void drawString(String text, int x, int y, int size, int screenWidth, int screenHeight, float r, float g, float b, float a, boolean ignoreColors) {
        if (text == null || text.isEmpty()) return;

        float scale = (float) size / GLYPH_SIZE;
        shader.use();
        shader.setInt("useTexture", 1);
        shader.setInt("useArray", 0);
        
        float currentR = r, currentG = g, currentB = b;
        boolean bold = false, rainbow = false, glow = false, wavy = false, shake = false;

        glActiveTexture(GL_TEXTURE0);
        glBindVertexArray(vao);

        int drawX = x;
        int lastBoundTexture = -1;
        long timeMs = System.currentTimeMillis();

        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int charCount = Character.charCount(cp);

            if (cp == '$' && i + charCount < text.length()) {
                int nextCp = text.codePointAt(i + charCount);
                int codeIndex = "0123456789abcdefklmnorgzvq".indexOf(Character.toLowerCase(nextCp));

                if (codeIndex >= 0 && codeIndex < 16) {
                    if (!ignoreColors) {
                        currentR = colorCodes[codeIndex][0];
                        currentG = colorCodes[codeIndex][1];
                        currentB = colorCodes[codeIndex][2];
                    }
                    rainbow = false; glow = false; wavy = false; shake = false;
                } else if (codeIndex == 17) { // l - bold
                    bold = true;
                } else if (codeIndex == 21) { // r - reset
                    currentR = r; currentG = g; currentB = b;
                    bold = false; rainbow = false; glow = false; wavy = false; shake = false;
                } else if (codeIndex == 22) { // g - glow
                    glow = true;
                } else if (codeIndex == 23) { // z - rainbow
                    if (!ignoreColors) rainbow = true;
                } else if (codeIndex == 24) { // v - wavy
                    wavy = true;
                } else if (codeIndex == 25) { // q - shake
                    shake = true;
                }
                i += charCount + Character.charCount(nextCp);
                continue;
            }

            float finalR = currentR, finalG = currentG, finalB = currentB;
            int finalX = drawX, finalY = y;

            if (rainbow) {
                float hue = ((timeMs % 3000) / 3000.0f + (drawX * 0.002f)) % 1.0f;
                org.joml.Vector3f rgb = hslToRgb(hue, 0.8f, 0.6f);
                finalR = rgb.x; finalG = rgb.y; finalB = rgb.z;
            }
            if (glow) {
                float pulse = (float)(Math.sin(timeMs * 0.005f) * 0.25f + 0.75f);
                finalR *= pulse; finalG *= pulse; finalB *= pulse;
            }
            if (wavy) finalY += (int)(Math.sin(timeMs * 0.008f + drawX * 0.05f) * 2.0f);
            if (shake) {
                finalX += (int)(Math.sin(timeMs * 0.1f + drawX) * 1.5f);
                finalY += (int)(Math.cos(timeMs * 0.13f + drawX * 0.5f) * 1.5f);
            }

            shader.setUniform("tintColor", finalR, finalG, finalB, a);

            int textureToBind;
            GlyphInfo glyph = glyphCustomMap.get(cp);
            if (glyph != null) {
                textureToBind = glyph.textureId;
            } else {
                int page = cp / 256;
                textureToBind = (page == 0 && cp < 128) ? getOrLoadTexture("zenith/textures/font/ascii.png") : getUnicodePageTexture(page);
            }
            
            if (textureToBind != lastBoundTexture) {
                glBindTexture(GL_TEXTURE_2D, textureToBind);
                lastBoundTexture = textureToBind;
            }

            int gw = (glyph != null) ? glyph.width : getGlyphWidth((char)cp);
            float advance = gw * scale;

            if (glyph != null) {
                renderCustomGlyph(glyph, finalX, finalY, size, screenWidth, screenHeight);
                if (bold) renderCustomGlyph(glyph, finalX + Math.round(0.5f * scale), finalY, size, screenWidth, screenHeight);
                drawX += Math.round(advance + scale);
            } else {
                int page = cp / 256;
                renderGlyph((char)cp, finalX, finalY, size, screenWidth, screenHeight);
                if (bold) renderGlyph((char)cp, finalX + Math.round(0.5f * scale), finalY, size, screenWidth, screenHeight);
                drawX += Math.round(page != 0 || cp >= 128 ? advance : advance + scale);
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
        int currentY = y, lineHeight = (int)(size * 1.3f); 
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
            } else currentLine = new StringBuilder(testLine);
        }
        if (currentLine.length() > 0) drawString(currentLine.toString(), x, currentY, size, screenWidth, screenHeight, r, g, b, a);
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
                if (currentLine.length() > 0) { lines++; currentLine = new StringBuilder(word); }
                else lines++;
            } else currentLine = new StringBuilder(testLine);
        }
        return lines * lineHeight;
    }

    private void renderCustomGlyph(GlyphInfo glyph, int x, int y, int size, int screenWidth, int screenHeight) {
        float uvStepX = 1.0f / glyph.totalCols, uvStepY = 1.0f / glyph.totalRows;
        float screenX = (2.0f * (x + size / 2.0f) / screenWidth) - 1.0f, screenY = 1.0f - (2.0f * (y + size / 2.0f) / screenHeight);
        shader.setUniform("scale", (float) size / screenWidth, (float) size / screenHeight, 0.0f, 0.0f);
        shader.setUniform("position_offset", screenX, screenY, 0.0f, 0.0f);
        shader.setUniform("uvOffset", glyph.gridX * uvStepX, glyph.gridY * uvStepY, 0.0f, 0.0f);
        shader.setUniform("uvScale", uvStepX, uvStepY, 0.0f, 0.0f);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    public int getStringWidth(String text, int size) {
        if (text == null || text.isEmpty()) return 0;
        float scale = (float) size / GLYPH_SIZE;
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i), charCount = Character.charCount(cp);
            if (cp == '$' && i + charCount < text.length()) {
                i += charCount + Character.charCount(text.codePointAt(i + charCount));
                continue;
            }
            GlyphInfo glyph = glyphCustomMap.get(cp);
            if (glyph != null) width += Math.round(glyph.width * scale + scale);
            else {
                int page = cp / 256;
                width += Math.round(getGlyphWidth((char)cp) * scale + (page == 0 && cp < 128 ? scale : 0));
            }
            i += charCount;
        }
        return width;
    }

    public java.util.List<String> wrapText(String text, int fontSize, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine.toString() + " " + word;
            if (getStringWidth(testLine, fontSize) <= maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) lines.add(currentLine.toString());
        return lines;
    }

    private org.joml.Vector3f hslToRgb(float h, float s, float l) {
        float q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
        return new org.joml.Vector3f(hueToRgb(p, q, h + 1.0f/3.0f), hueToRgb(p, q, h), hueToRgb(p, q, h - 1.0f/3.0f));
    }

    private float hueToRgb(float p, float q, float t) {
        if (t < 0) t += 1; if (t > 1) t -= 1;
        if (t < 1.0f/6.0f) return p + (q - p) * 6 * t;
        if (t < 1.0f/2.0f) return q;
        if (t < 2.0f/3.0f) return p + (q - p) * (2.0f/3.0f - t) * 6;
        return p;
    }

    private int getUnicodePageTexture(int page) {
        if (unicodePages.containsKey(page)) return unicodePages.get(page);
        String path = "zenith/textures/font/unicode_page_" + String.format("%02x", page) + ".png";
        loadUnicodeWidthMap(path, page * 256);
        int tid = loadTexture(path); unicodePages.put(page, tid); return tid;
    }

    private int loadTexture(String path) {
        try (InputStream stream = FontRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return 0;
            BufferedImage img = ImageIO.read(stream);
            int w = img.getWidth(), h = img.getHeight(), pixels[] = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);
            ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
            for (int p : pixels) { buf.put((byte)((p>>16)&0xFF)); buf.put((byte)((p>>8)&0xFF)); buf.put((byte)(p&0xFF)); buf.put((byte)((p>>24)&0xFF)); }
            buf.flip();
            int tid = glGenTextures(); glBindTexture(GL_TEXTURE_2D, tid);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            MemoryUtil.memFree(buf); return tid;
        } catch (IOException e) { return 0; }
    }

    private void renderGlyph(char c, int x, int y, int size, int screenWidth, int screenHeight) {
        int idx = c % 256, gx = idx % GRID_SIZE, gy = idx / GRID_SIZE;
        float uvS = 1.0f / GRID_SIZE, sx = (2.0f * (x + size / 2.0f) / screenWidth) - 1.0f, sy = 1.0f - (2.0f * (y + size / 2.0f) / screenHeight);
        shader.setUniform("scale", (float) size / screenWidth, (float) size / screenHeight, 0.0f, 0.0f);
        shader.setUniform("position_offset", sx, sy, 0.0f, 0.0f);
        shader.setUniform("uvOffset", gx * uvS, gy * uvS, 0.0f, 0.0f);
        shader.setUniform("uvScale", uvS, uvS, 0.0f, 0.0f);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    private void loadWidthMap(String path, int[] targetMap) {
        try (InputStream stream = FontRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return;
            BufferedImage img = ImageIO.read(stream);
            int w = img.getWidth(), h = img.getHeight(), pxs[] = new int[w * h];
            img.getRGB(0, 0, w, h, pxs, 0, w);
            int cw = w/16, ch = h/16;
            for (int i=0; i<256; i++) {
                int col = i%16, row = i/16, maxC = cw-1; boolean empty;
                do { empty = true; for (int py=0; py<ch; py++) if (((pxs[(row*ch+py)*w + (col*cw+maxC)]>>24)&0xFF) > 0x80) { empty=false; break; }
                if (empty && maxC>=0) maxC--; } while (empty && maxC>=0);
                targetMap[i] = (i==32) ? 4 : maxC+2;
            }
        } catch (IOException e) { Logger.error("Failed to load width map: " + path); }
    }

    private void loadUnicodeWidthMap(String path, int start) {
        try (InputStream s = FontRenderer.class.getClassLoader().getResourceAsStream(path)) {
            if (s == null) return;
            BufferedImage img = ImageIO.read(s);
            int w = img.getWidth(), h = img.getHeight(), pxs[] = new int[w * h];
            img.getRGB(0, 0, w, h, pxs, 0, w);
            int cw = w/16, ch = h/16;
            for (int i=0; i<256; i++) {
                int col = i%16, row = i/16, maxC = cw-1; boolean empty;
                do { empty = true; for (int py=0; py<ch; py++) if (((pxs[(row*ch+py)*w + (col*cw+maxC)]>>24)&0xFF) > 0x20) { empty=false; break; }
                if (empty && maxC>=0) maxC--; } while (empty && maxC>=0);
                unicodeWidthMap[start+i] = maxC<0 ? 0 : maxC+2;
            }
        } catch (IOException e) { Logger.error("Failed to load unicode width map: " + path); }
    }

    private int getGlyphWidth(char c) { return c < 256 ? widthMap[c] : (unicodeWidthMap[c] > 0 ? unicodeWidthMap[c] : 6); }

    private void createQuad() {
        vao = glGenVertexArrays(); vbo = glGenBuffers(); ebo = glGenBuffers();
        glBindVertexArray(vao);
        float[] vs = {-1,-1,0,1, 1,-1,1,1, 1,1,1,0, -1,1,0,0};
        FloatBuffer vb = MemoryUtil.memAllocFloat(vs.length); vb.put(vs).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo); glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4*Float.BYTES, 0); glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4*Float.BYTES, 2*Float.BYTES); glEnableVertexAttribArray(1);
        int[] is = {0,1,2, 2,3,0}; IntBuffer ib = MemoryUtil.memAllocInt(is.length); ib.put(is).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo); glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        glBindVertexArray(0); MemoryUtil.memFree(vb); MemoryUtil.memFree(ib);
    }

    public void cleanup() {
        for (int t : textureCache.values()) glDeleteTextures(t);
        for (int t : unicodePages.values()) glDeleteTextures(t);
        glDeleteVertexArrays(vao); glDeleteBuffers(vbo); glDeleteBuffers(ebo);
    }
}
