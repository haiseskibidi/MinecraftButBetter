package com.za.zenith.engine.graphics;

import com.za.zenith.utils.Logger;
import com.za.zenith.world.World;
import com.za.zenith.world.WorldSettings;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * RenderContext manages global shader data using Uniform Buffer Objects (UBO).
 * This eliminates the need to manually set common uniforms (time, light, matrices) in every shader.
 */
public class RenderContext {
    public static final int GLOBAL_BINDING_POINT = 0;
    private static int uboId;
    private static final FloatBuffer uboBuffer = BufferUtils.createFloatBuffer(64); // Enough for 2 mats + vectors + floats

    private static float time;
    private static final Vector3f sunDirection = new Vector3f();
    private static final Vector3f ambientColor = new Vector3f();
    private static final Matrix4f viewMatrix = new Matrix4f();
    private static final Matrix4f projectionMatrix = new Matrix4f();
    private static final Vector3f cameraPos = new Vector3f();
    private static final Vector3f grassColor = new Vector3f();

    public static void init() {
        uboId = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, uboId);
        // Size: 2 * 64 (matrices) + 4 * 16 (vectors/floats with padding) = 192 bytes
        glBufferData(GL_UNIFORM_BUFFER, 256, GL_DYNAMIC_DRAW); 
        glBindBufferBase(GL_UNIFORM_BUFFER, GLOBAL_BINDING_POINT, uboId);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    public static void update(World world, Camera camera, float alpha, Vector3f lightDir, Vector3f ambient) {
        time = (float) (org.lwjgl.glfw.GLFW.glfwGetTime() % 3600.0);
        sunDirection.set(lightDir);
        ambientColor.set(ambient);
        viewMatrix.set(camera.getViewMatrix(alpha));
        projectionMatrix.set(camera.getProjectionMatrix());
        cameraPos.set(camera.getPosition());
        grassColor.set(com.za.zenith.engine.graphics.ColorProvider.getGrassColor());

        sync();
    }

    private static void sync() {
        uboBuffer.clear();
        
        // 0-15: Projection Matrix
        projectionMatrix.get(0, uboBuffer);
        
        // 16-31: View Matrix
        viewMatrix.get(16, uboBuffer);
        
        // 32-35: Camera Data (xyz=pos, w=0)
        uboBuffer.put(32, cameraPos.x);
        uboBuffer.put(33, cameraPos.y);
        uboBuffer.put(34, cameraPos.z);
        uboBuffer.put(35, 0.0f);
        
        // 36-39: Sun Data (xyz=dir, w=time)
        uboBuffer.put(36, sunDirection.x);
        uboBuffer.put(37, sunDirection.y);
        uboBuffer.put(38, sunDirection.z);
        uboBuffer.put(39, time);
        
        // 40-43: Ambient Data (xyz=color, w=0)
        uboBuffer.put(40, ambientColor.x);
        uboBuffer.put(41, ambientColor.y);
        uboBuffer.put(42, ambientColor.z);
        uboBuffer.put(43, 0.0f);
        
        // 44-47: Grass Data (xyz=color, w=0)
        uboBuffer.put(44, grassColor.x);
        uboBuffer.put(45, grassColor.y);
        uboBuffer.put(46, grassColor.z);
        uboBuffer.put(47, 0.0f);

        // Limit the buffer to the used portion
        uboBuffer.position(0);
        uboBuffer.limit(48);

        glBindBuffer(GL_UNIFORM_BUFFER, uboId);
        glBufferSubData(GL_UNIFORM_BUFFER, 0, uboBuffer);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }

    public static void bindShader(Shader shader) {
        int blockIndex = glGetUniformBlockIndex(shader.getProgramId(), "GlobalData");
        if (blockIndex != -1) {
            glUniformBlockBinding(shader.getProgramId(), blockIndex, GLOBAL_BINDING_POINT);
            Logger.info("Bound shader program " + shader.getProgramId() + " to GlobalData UBO");
        } else {
            // Some shaders (like UI) might not use global data, which is fine
        }
    }

    public static void cleanup() {
        glDeleteBuffers(uboId);
    }
}
