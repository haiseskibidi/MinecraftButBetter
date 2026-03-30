package com.za.minecraft.world.items;

import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.stb.STBImage.*;

public class ItemMeshGenerator {

    public static Mesh generateItemMesh(String texturePath, DynamicTextureAtlas atlas, int itemId) {
        if (texturePath == null || texturePath.isEmpty() || atlas == null) return null;
        
        String resourcePath = texturePath.replace("src/main/resources/", "");
        ByteBuffer image = null;
        int width, height;

        try (var is = ItemMeshGenerator.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            byte[] data = is.readAllBytes();
            
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            
            int[] w = new int[1];
            int[] h = new int[1];
            int[] c = new int[1];
            stbi_set_flip_vertically_on_load(true);
            image = stbi_load_from_memory(buffer, w, h, c, 4);
            MemoryUtil.memFree(buffer);
            
            if (image == null) return null;
            width = w[0];
            height = h[0];
        } catch (Exception e) {
            if (image != null) stbi_image_free(image);
            return null;
        }

        // --- Анализ текстуры (PCA) для автоматической ориентации ---
        List<int[]> pixels = new ArrayList<>();
        double meanX = 0, meanY = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isOpaque(image, x, y, width, height)) {
                    pixels.add(new int[]{x, y});
                    meanX += x;
                    meanY += y;
                }
            }
        }

        if (pixels.isEmpty()) {
            stbi_image_free(image);
            return null;
        }

        int N = pixels.size();
        meanX /= N;
        meanY /= N;

        double varX = 0, varY = 0, covXY = 0;
        for (int[] p : pixels) {
            double dx = p[0] - meanX;
            double dy = p[1] - meanY;
            varX += dx * dx;
            varY += dy * dy;
            covXY += dx * dy;
        }
        
        // Угол главной оси (theta)
        double theta = 0.5 * Math.atan2(2 * covXY, varX - varY);
        
        // Вычисляем собственные значения для определения "вытянутости" (eccentricity)
        double trace = varX + varY;
        double det = varX * varY - covXY * covXY;
        double sqrtDisc = Math.sqrt(trace * trace / 4.0 - det);
        double lambda1 = trace / 2.0 + sqrtDisc;
        double lambda2 = trace / 2.0 - sqrtDisc;
        double eccentricity = (lambda1 - lambda2) / (lambda1 + lambda2 + 1e-6);

        // Нам нужно, чтобы предмет стоял ВЕРТИКАЛЬНО.
        // rotationAngle: на сколько довернуть, чтобы главная ось стала вертикальной (PI/2).
        float rotationAngle = (float) (Math.PI / 2.0 - theta);

        // Эвристика: 
        // 1. Если предмет почти квадратный (eccentricity < 0.3), не крутим его.
        // 2. Если он уже почти вертикальный (rotationAngle очень мал), не крутим.
        if (eccentricity < 0.3 || Math.abs(Math.sin(rotationAngle)) < 0.1) {
            rotationAngle = 0;
        }
        
        float cosR = (float) Math.cos(rotationAngle);
        float sinR = (float) Math.sin(rotationAngle);

        // Определяем "нижнюю" точку (точку хвата)
        // Для повернутых предметов это экстремум вдоль оси.
        // Для не повернутых - просто самый низ текстуры.
        double minVal = Double.MAX_VALUE;
        double gx = meanX, gy = meanY; // Дефолт в центр масс
        
        if (rotationAngle != 0) {
            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);
            for (int[] p : pixels) {
                double val = p[0] * cosT + p[1] * sinT;
                if (val < minVal) {
                    minVal = val;
                    gx = p[0]; gy = p[1];
                }
            }
        } else {
            // Предмет не вращаем - хват в центре нижней границы
            double minY = Double.MAX_VALUE;
            for (int[] p : pixels) {
                if (p[1] < minY) {
                    minY = p[1];
                    gx = p[0]; // Будет уточнено средним ниже
                }
            }
            // Уточняем gx как среднее всех пикселей на нижней линии
            double sumX = 0; int countX = 0;
            for (int[] p : pixels) {
                if (Math.abs(p[1] - minY) < 1.0) {
                    sumX += p[0]; countX++;
                }
            }
            gx = sumX / countX;
            gy = minY;
        }

        // gx, gy теперь координаты пикселя "хвата" (в единицах 0..width)
        float fgx = (float) gx / width;
        float fgy = (float) gy / height;

        // --- AAA Polish: Центрирование меша ---
        // Чтобы предметы в мире (ItemEntity) лежали ровно по центру хитбокса,
        // мы должны генерировать вершины относительно геометрического центра текстуры,
        // а не точки хвата.
        float minX = width, minY = height, maxX = 0, maxY = 0;
        for (int[] p : pixels) {
            minX = Math.min(minX, p[0]); minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]); maxY = Math.max(maxY, p[1]);
        }
        float centerX = (minX + maxX + 1) * 0.5f / width;
        float centerY = (minY + maxY + 1) * 0.5f / height;

        // Смещение точки хвата относительно центра меша
        org.joml.Vector3f graspOffset = new org.joml.Vector3f(fgx - centerX, fgy - centerY, 0);

        // --- Генерация меша ---
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float thickness = 0.0625f; 
        float h = thickness / 2.0f;
        
        float[] uvs = atlas.uvFor(texturePath);
        float layer = uvs[2];
        float u0 = 0, v0 = 0, u1 = 1, v1 = 1;
        float uSize = u1 - u0, vSize = v1 - v0;
        float uE = 0.0005f, vE = 0.0005f;

        for (int[] p : pixels) {
            int x = p[0], y = p[1];
            float pu0 = u0 + (float) x / width * uSize + uE;
            float pv0 = v0 + (float) y / height * vSize + vE; 
            float pu1 = u0 + (float) (x + 1) / width * uSize - uE;
            float pv1 = v0 + (float) (y + 1) / height * vSize - vE;

            float mu = (pu0 + pu1) * 0.5f;
            float mv = (pv0 + pv1) * 0.5f;

            // Координаты относительно геометрического центра
            float bx0 = (float) x / width - centerX;
            float by0 = (float) y / height - centerY;
            float bx1 = (float) (x + 1) / width - centerX;
            float by1 = (float) (y + 1) / height - centerY;

            // Поворот вершин в меше (вокруг центра)
            float px00 = bx0 * cosR - by0 * sinR; float py00 = bx0 * sinR + by0 * cosR;
            float px10 = bx1 * cosR - by0 * sinR; float py10 = bx1 * sinR + by0 * cosR;
            float px11 = bx1 * cosR - by1 * sinR; float py11 = bx1 * sinR + by1 * cosR;
            float px01 = bx0 * cosR - by1 * sinR; float py01 = bx0 * sinR + by1 * cosR;

            // FRONT (+Z)
            addQuad(positions, texCoords, normals, indices,
                px00, py00, h,  px10, py10, h,  px11, py11, h,  px01, py01, h,
                pu0, pv0, layer, pu1, pv0, layer, pu1, pv1, layer, pu0, pv1, layer, 0, 0, 1);
            // BACK (-Z)
            addQuad(positions, texCoords, normals, indices,
                px00, py00, -h,  px01, py01, -h,  px11, py11, -h,  px10, py10, -h,
                pu0, pv0, layer, pu0, pv1, layer, pu1, pv1, layer, pu1, pv0, layer, 0, 0, -1);
            
            // Торцы (sides)
            if (!isOpaque(image, x, y + 1, width, height)) {
                addQuad(positions, texCoords, normals, indices, px01, py01, h, px11, py11, h, px11, py11, -h, px01, py01, -h, mu, mv, layer, mu, mv, layer, mu, mv, layer, mu, mv, layer, 0, 1, 0);
            }
            if (!isOpaque(image, x, y - 1, width, height)) {
                addQuad(positions, texCoords, normals, indices, px00, py00, -h, px10, py10, -h, px10, py10, h, px00, py00, h, mu, mv, layer, mu, mv, layer, mu, mv, layer, mu, mv, layer, 0, -1, 0);
            }
            if (!isOpaque(image, x - 1, y, width, height)) {
                addQuad(positions, texCoords, normals, indices, px00, py00, -h, px00, py00, h, px01, py01, h, px01, py01, -h, mu, mv, layer, mu, mv, layer, mu, mv, layer, mu, mv, layer, -1, 0, 0);
            }
            if (!isOpaque(image, x + 1, y, width, height)) {
                addQuad(positions, texCoords, normals, indices, px10, py10, h, px10, py10, -h, px11, py11, -h, px11, py11, h, mu, mv, layer, mu, mv, layer, mu, mv, layer, mu, mv, layer, 1, 0, 0);
            }
        }

        stbi_image_free(image);

        float[] posArr = new float[positions.size()];
        for (int i = 0; i < positions.size(); i++) posArr[i] = positions.get(i);
        float[] texArr = new float[texCoords.size()];
        for (int i = 0; i < texCoords.size(); i++) texArr[i] = texCoords.get(i);
        float[] normArr = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) normArr[i] = normals.get(i);
        int[] indArr = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) indArr[i] = indices.get(i);

        float[] blockTypes = new float[posArr.length / 3];
        for (int i = 0; i < blockTypes.length; i++) blockTypes[i] = itemId;

        Mesh finalMesh = new Mesh(posArr, texArr, normArr, blockTypes, indArr);
        finalMesh.setGraspOffset(graspOffset);
        return finalMesh;
    }

    private static boolean isOpaque(ByteBuffer image, int x, int y, int width, int height) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        int alpha = image.get((y * width + x) * 4 + 3) & 0xFF;
        return alpha > 30; 
    }

    private static void addQuad(List<Float> pos, List<Float> tex, List<Float> norm, List<Integer> ind,
                               float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               float u0, float v0, float w0, 
                               float u1, float v1, float w1,
                               float u2, float v2, float w2,
                               float u3, float v3, float w3,
                               float nx, float ny, float nz) {
        int start = pos.size() / 3;
        pos.add(x0); pos.add(y0); pos.add(z0);
        pos.add(x1); pos.add(y1); pos.add(z1);
        pos.add(x2); pos.add(y2); pos.add(z2);
        pos.add(x3); pos.add(y3); pos.add(z3);
        tex.add(u0); tex.add(v0); tex.add(w0);
        tex.add(u1); tex.add(v1); tex.add(w1);
        tex.add(u2); tex.add(v2); tex.add(w2);
        tex.add(u3); tex.add(v3); tex.add(w3);
        for (int i = 0; i < 4; i++) {
            norm.add(nx); norm.add(ny); norm.add(nz);
        }
        ind.add(start); ind.add(start + 1); ind.add(start + 2);
        ind.add(start + 2); ind.add(start + 3); ind.add(start);
    }
}
