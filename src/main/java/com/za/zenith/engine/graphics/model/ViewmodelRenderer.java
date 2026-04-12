package com.za.zenith.engine.graphics.model;

import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.engine.graphics.Mesh;
import com.za.zenith.world.items.ItemStack;
import org.joml.Matrix4f;

/**
 * Рендерер для отображения рук игрока и предметов в них.
 */
public class ViewmodelRenderer {
    private final HeldItemRenderer heldItemRenderer = new HeldItemRenderer();

    public HeldItemRenderer getHeldItemRenderer() {
        return heldItemRenderer;
    }

    public void render(Viewmodel viewmodel, Shader shader, DynamicTextureAtlas atlas, com.za.zenith.entities.Player player, ItemStack mainHand, ItemStack offHand, float handHeat, float itemHeat) {
        if (viewmodel == null) return;

        shader.use();
        atlas.bind();

        // 1. Отрисовка скелета рук
        renderNode(viewmodel.root, shader, handHeat);

        // 2. Отрисовка предметов в сокетах (Data-Driven Sockets)
        renderSockets(viewmodel.root, shader, atlas, mainHand, offHand, itemHeat);
    }

    private void renderSockets(ModelNode node, Shader shader, DynamicTextureAtlas atlas, ItemStack mainHand, ItemStack offHand, float itemHeat) {
        if (node.name.startsWith("socket_")) {
            // Check Main Hand
            renderItemInSocket(node, mainHand, shader, atlas, true, itemHeat);
            // Check Off Hand
            renderItemInSocket(node, offHand, shader, atlas, false, itemHeat);
        }

        for (ModelNode child : node.children) {
            renderSockets(child, shader, atlas, mainHand, offHand, itemHeat);
        }
    }

    private void renderItemInSocket(ModelNode socketNode, ItemStack stack, Shader shader, DynamicTextureAtlas atlas, boolean isMainHand, float itemHeat) {
        if (stack == null) return;
        
        com.za.zenith.world.items.Item item = stack.getItem();
        com.za.zenith.world.items.component.ViewmodelComponent vmComp = 
            item.getComponent(com.za.zenith.world.items.component.ViewmodelComponent.class);
        
        String targetSocket;
        if (vmComp != null && vmComp.socket() != null) {
            targetSocket = vmComp.socket();
            if (!targetSocket.endsWith("_r") && !targetSocket.endsWith("_l")) {
                targetSocket += (isMainHand ? "_r" : "_l");
            }
        } else {
            targetSocket = isMainHand ? "socket_palm_r" : "socket_palm_l";
        }
        
        if (socketNode.name.equals(targetSocket)) {
            org.joml.Matrix4f transform = new org.joml.Matrix4f(socketNode.globalMatrix);
            
            float tx, ty, tz, rx, ry, rz, scale;
            
            if (vmComp != null) {
                tx = (vmComp.translation()[0] / 16.0f) * (isMainHand ? 1.0f : -1.0f);
                ty = vmComp.translation()[1] / 16.0f;
                tz = vmComp.translation()[2] / 16.0f;
                rx = vmComp.rotation()[0];
                ry = vmComp.rotation()[1] * (isMainHand ? 1.0f : -1.0f);
                rz = vmComp.rotation()[2] * (isMainHand ? 1.0f : -1.0f);
                scale = vmComp.scale();
            } else {
                if (item.isBlock()) {
                    // Выносим блок из кисти (сдвигаем влево для правой руки, вправо для левой)
                    tx = isMainHand ? -0.15f : 0.15f; 
                    ty = 0.15f; tz = 0;
                    rx = 30; ry = isMainHand ? 15 : -15; rz = 0;
                    scale = 0.4f;
                } else {
                    tx = isMainHand ? -0.05f : 0.05f; 
                    ty = -0.1f; tz = 0;
                    rx = 0; ry = isMainHand ? -90 : 90; rz = 0;
                    scale = 0.85f;
                }
            }

            transform.translate(tx, ty, tz);
            transform.rotateXYZ((float)Math.toRadians(rx), (float)Math.toRadians(ry), (float)Math.toRadians(rz));
            transform.scale(scale);
            
            if (!item.isBlock()) {
                Mesh itemMesh = heldItemRenderer.getOrGenerateMesh(item, atlas);
                if (itemMesh != null) {
                    org.joml.Vector3f go = itemMesh.getGraspOffset();
                    transform.translate(-go.x, -go.y, -go.z);
                }
            }
            
            heldItemRenderer.render(transform, stack, shader, atlas, itemHeat);
        }
    }

    private ModelNode findNode(ModelNode root, String name) {
        if (root.name.equals(name)) return root;
        for (ModelNode child : root.children) {
            ModelNode found = findNode(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private void renderNode(ModelNode node, Shader shader, float heat) {
        if (node.mesh != null) {
            shader.setMatrix4f("model", node.globalMatrix);
            
            float partWeight = 0.0f;
            if (node.name.contains("hand") || node.name.contains("finger")) partWeight = 1.0f;
            else if (node.name.contains("forearm")) partWeight = 0.6f;
            else if (node.name.contains("shoulder")) partWeight = 0.3f;

            shader.setBoolean("isHand", partWeight > 0.01f);
            shader.setFloat("uHandPartWeight", partWeight);
            shader.setFloat("uMiningHeat", heat);
            
            node.mesh.render();
            shader.setBoolean("isHand", false);
        }
        for (ModelNode child : node.children) {
            renderNode(child, shader, heat);
        }
    }

    public void cleanup() {
        heldItemRenderer.cleanup();
    }
}


