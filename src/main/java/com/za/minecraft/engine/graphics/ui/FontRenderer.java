package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.utils.Logger;
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
    private static final float GLYPH_UV_SIZE = 7.99f;

    private final int[] widthMap = new int[256];

    private Shader shader;
    private int textureId;
    private int vao;
    private int vbo;
    private int ebo;

    public void init(Shader shader) {
        this.shader = shader;
        loadFontTexture();
        createQuad();
    }

    public void drawString(String text, int x, int y, int size, int screenWidth, int screenHeight) {
        if (text == null || text.isEmpty()) {
            return;
        }

        float scale = (float) size / GLYPH_SIZE;

        shader.use();
        shader.setInt("useTexture", 1);
        shader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        glBindTexture(GL_TEXTURE_2D, textureId);
        glBindVertexArray(vao);

        int drawX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) { // цветовые коды, пропускаем
                i++;
                continue;
            }

            int glyphWidth = getGlyphWidth(c);
            float advance = glyphWidth * scale;

            renderGlyph(c, drawX, y, size, screenWidth, screenHeight);
            drawX += Math.round(advance + scale); // небольшой отступ как в оригинале
        }

        glBindVertexArray(0);
    }

    public int getStringWidth(String text, int size) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float scale = (float) size / GLYPH_SIZE;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            width += Math.round(getGlyphWidth(c) * scale + scale);
        }
        return width;
    }

    public void cleanup() {
        glDeleteTextures(textureId);
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }

    private void renderGlyph(char c, int x, int y, int size, int screenWidth, int screenHeight) {
        float scale = (float) size / GLYPH_SIZE;

        int glyphIndex = c & 0xFF;
        int gridX = glyphIndex % GRID_SIZE;
        int gridY = glyphIndex / GRID_SIZE;

        float uvX = (gridX * GLYPH_SIZE) / (float) (GRID_SIZE * GLYPH_SIZE);
        float uvY = (gridY * GLYPH_SIZE) / (float) (GRID_SIZE * GLYPH_SIZE);
        float uvSize = GLYPH_UV_SIZE / (GRID_SIZE * GLYPH_SIZE);

        float screenX = (2.0f * (x + size / 2.0f) / screenWidth) - 1.0f;
        float screenY = 1.0f - (2.0f * (y + size / 2.0f) / screenHeight);

        shader.setUniform("scale", (float) size / screenWidth, (float) size / screenHeight, 0.0f, 0.0f);
        shader.setUniform("position_offset", screenX, screenY, 0.0f, 0.0f);
        shader.setUniform("uvOffset", uvX, uvY, 0.0f, 0.0f);
        shader.setUniform("uvScale", uvSize, uvSize, 0.0f, 0.0f);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    private void loadFontTexture() {
        try (InputStream stream = FontRenderer.class.getClassLoader().getResourceAsStream("textures/default.png")) {
            if (stream == null) {
                throw new IllegalStateException("Missing font texture: textures/default.png");
            }
            BufferedImage image = ImageIO.read(stream);
            int width = image.getWidth();
            int height = image.getHeight();

            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            computeWidthMap(pixels, width);

            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
            for (int pixel : pixels) {
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
            buffer.flip();

            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            MemoryUtil.memFree(buffer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load Minecraft font", e);
        }
    }

    private void computeWidthMap(int[] pixels, int imageWidth) {
        for (int i = 0; i < 128; i++) {
            int col = i % GRID_SIZE;
            int row = i / GRID_SIZE;
            int maxColumn = GLYPH_SIZE - 1;
            boolean emptyColumn;
            do {
                emptyColumn = true;
                for (int y = 0; y < GLYPH_SIZE; y++) {
                    int pixelIndex = (row * GLYPH_SIZE + y) * imageWidth + (col * GLYPH_SIZE + maxColumn);
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

            if (i == 32) {
                widthMap[i] = 4;
            } else {
                widthMap[i] = maxColumn + 2;
            }
        }

        for (int i = 128; i < 256; i++) {
            widthMap[i] = GLYPH_SIZE;
        }
    }

    private void createQuad() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        float[] vertices = {
            -1.0f, -1.0f, 0.0f, 0.0f,
             1.0f, -1.0f, 1.0f, 0.0f,
             1.0f,  1.0f, 1.0f, 1.0f,
            -1.0f,  1.0f, 0.0f, 1.0f
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

    private int getGlyphWidth(char c) {
        int index = c & 0xFF;
        int width = widthMap[index];
        if (width == 0) {
            return 4;
        }
        return width;
    }
}
